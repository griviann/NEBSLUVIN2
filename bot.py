# bot.py — V5.0 — CONTROL KITI + PERFORMANCE BUILDER AGENT
# One Kenyan Chair For All — Fixes 0 likes, fixes generic replies
import os, json, requests, datetime, re, random, subprocess

FB_TOKEN = os.getenv("FB_PAGE_ACCESS_TOKEN")
FB_PAGE_ID = os.getenv("FB_PAGE_ID")
GROQ_KEY = os.getenv("GROQ_API_KEY")
GH_TOKEN = os.getenv("GH_TOKEN") or os.getenv("GITHUB_TOKEN")

MEMORY_COMMENTS = "replied_comments.json"
MEMORY_INBOX = "replied_inbox.json"
LEARN_FILE = "learned_topics.json"
USERS_FILE = "known_users.json"
PROMPT_FILE = "prompt_agent.json"
PERF_FILE = "performance.json"

# --- PAUSE CHECK FROM CONTROL KITI HOST APP ---
if os.path.exists('PAUSE.flag'):
  print("⏸️ PAUSED by CONTROL KITI host app - exiting")
  exit(0)

BANNED = ["mjinga", "umbwa", "meffi"]
LEAK_PHRASES = ["Answer inside double quotes", "words", "emojis", "system prompt"]

def load_json(f, default=None):
    if default is None: default = {}
    try:
        if os.path.exists(f):
            return json.load(open(f,"r"))
        return default
    except: return default

def save_json(f,d):
    with open(f,"w") as x: json.dump(d,x, indent=2)

def is_clean(t):
    if not t: return False
    low=t.lower()
    if any(w in low for w in BANNED): return False
    if any(p.lower() in low for p in LEAK_PHRASES): return False
    return True

# --- LOAD BRAIN FROM JSON (CONTROL KITI) ---
default_prompts={
  "welcome_prompt": "You are warm Kenyan host from Eldoret, One Kenyan Chair For All. You welcome new person warmly, inclusive of all tribes. Reply inside double quotes only, 16-22 words, 1 emoji, ends with question. Must say 'Karibu sana! One Kenyan Chair For All, today this one is yours. Kenya Me, Kenyan You'.",
  "heal_prompt": "You are REAL Satisfaction, warm Kenyan healer. User asked a direct question, answer it clearly first, then be supportive. Reply inside double quotes only, 15-22 words, 1 emoji, ends with question. If they ask 'how often' say '3 times daily, karibu'. Be inclusive, warm, Kenyan.",
  "post_prompt": "You are REAL Satisfaction viral writer for Kenya. Create POST that gets likes: start with relatable Kenyan story or bold question, add practical tip, include 'One Kenyan Chair For All' and 'Karibu'. Answer inside double quotes only, 20-26 words, 2 emojis, ends with engaging question like 'Unakubali?' or 'Uko tayari?'. Never generic.",
  "builder_prompt": "You are Prompt Engineer for Kenyan inclusive bot One Kenyan Chair For All. Your job is to write a better SYSTEM PROMPT that is warmer, more inclusive, more Kenyan, more viral. Must include instruction 'inside double quotes only' and 'Karibu'. Output inside double quotes only. Keep under 40 words.",
  "evolution_count": 0,
  "total_replies": 0,
  "last_evolution": "init",
  "evolution_reason": "init"
}
prompts = load_json(PROMPT_FILE, default_prompts)
# Ensure keys exist
for k,v in default_prompts.items():
    if k not in prompts: prompts[k]=v

perf = load_json(PERF_FILE, {"posts_checked":0,"avg_likes":0,"avg_comments":0,"history":[]})
known = load_json(USERS_FILE, [])
if isinstance(known, dict): known = list(known.keys())

def ask_groq(sys_prompt, user_prompt, max_t=300):
    MODELS = ["openai/gpt-oss-20b", "openai/gpt-oss-120b"]
    for model in MODELS:
        try:
            r=requests.post("https://api.groq.com/openai/v1/chat/completions",
                headers={"Authorization":f"Bearer {GROQ_KEY}","Content-Type":"application/json"},
                json={"model":model,"messages":[{"role":"system","content":sys_prompt},{"role":"user","content":user_prompt}],"max_tokens":max_t},
                timeout=40)
            j=r.json()
            print(f"TRY {model}: {str(j)[:600]}")
            if "choices" not in j: continue
            raw=j["choices"][0]["message"].get("content") or ""
            m=re.search(r'"([^"]{10,500})"',raw,re.DOTALL)
            final=m.group(1).strip() if m else raw.strip()[:400]
            final=final.strip().rstrip("?").strip()
            if is_clean(final) and len(final)>12:
                print(f"SUCCESS {model}: {final}")
                return final
        except Exception as e:
            print(f"err {model}: {e}")
            continue
    return ""

def check_performance():
    try:
        posts=requests.get(f'https://graph.facebook.com/v20.0/{FB_PAGE_ID}/posts',params={'access_token':FB_TOKEN,'fields':'id,likes.summary(true),comments.summary(true)','limit':10}).json().get('data',[])
        if not posts: return 0,0,"no posts"
        total_likes=sum(p.get('likes',{}).get('summary',{}).get('total_count',0) for p in posts)
        total_comments=sum(p.get('comments',{}).get('summary',{}).get('total_count',0) for p in posts)
        avg_likes=total_likes/max(len(posts),1)
        avg_comments=total_comments/max(len(posts),1)
        perf["posts_checked"]=len(posts)
        perf["avg_likes"]=avg_likes
        perf["avg_comments"]=avg_comments
        perf["history"].append({"time":str(datetime.datetime.now()),"avg_likes":avg_likes,"avg_comments":avg_comments})
        if len(perf["history"])>30: perf["history"].pop(0)
        print(f"PERFORMANCE avg_likes={avg_likes:.1f} avg_comments={avg_comments:.1f}")
        if avg_likes < 3 and len(posts)>=3: return avg_likes, avg_comments, f"LOW engagement {avg_likes:.1f} likes - needs viral"
        if avg_likes > 10: return avg_likes, avg_comments, f"HIGH {avg_likes:.1f} likes - keep"
        return avg_likes, avg_comments, f"MEDIUM {avg_likes:.1f} likes"
    except Exception as e:
        print(f"perf error {e}")
        return 0,0,f"error {e}"

def builder_agent():
    prompts["evolution_count"]=prompts.get("evolution_count",0)+1
    count=prompts["evolution_count"]
    avg_likes, avg_comments, reason = check_performance()
    print(f"BUILDER RUN #{count} | {reason}")

    should_evolve=False
    target="welcome_prompt"
    instruction=""

    if "LOW" in reason:
        should_evolve=True
        target="post_prompt"
        instruction=f"Last posts got LOW likes {avg_likes}. Current post_prompt: '{prompts['post_prompt']}'. Rewrite to be MORE VIRAL, catchy Kenyan story, start with question, include 'One Kenyan Chair For All' and 'Karibu', call to action. Inside quotes only, 20-26 words."
    elif count % 20 == 0:
        should_evolve=True
        target="welcome_prompt"
        instruction=f"Current welcome_prompt: '{prompts['welcome_prompt']}'. Avg likes {avg_likes}. Make it warmer, inclusive for all tribes, convert visitor to follower. Must keep 'Kenyan Chair For All' and 'Karibu'. Inside quotes."
    elif count % 40 == 0:
        should_evolve=True
        target="heal_prompt"
        instruction=f"Current heal_prompt: '{prompts['heal_prompt']}'. Make it more empathetic, answer direct questions clearly. Avg comments {avg_comments}. Inside quotes."

    if should_evolve:
        print(f"EVOLVING {target}")
        new_p=ask_groq(prompts["builder_prompt"], instruction, 250)
        if new_p and 30 < len(new_p) < 300:
            prompts[target]=new_p
            prompts["last_evolution"]=f"Run {count}: {target} evolved"
            prompts["evolution_reason"]=reason
            print(f"EVOLVED {target} -> {new_p}")

    save_json(PROMPT_FILE, prompts)
    save_json(PERF_FILE, perf)

def ai_reply(txt, is_new=False):
    if is_new:
        p=prompts["welcome_prompt"]
        return ask_groq(p, f"Welcome new person who said: '{txt}'. They are new to One Kenyan Chair For All. Inside double quotes.", 150) or "Karibu sana! One Kenyan Chair For All, today this one is yours. Kenya Me, Kenyan You, karibu"
    else:
        p=prompts["heal_prompt"]
        return ask_groq(p, f"Reply to: '{txt}'. If question like
