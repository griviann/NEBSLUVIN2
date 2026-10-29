# bot.py — V3.3 — FIXED - Free Groq models Oct 2026
import os, json, requests, datetime, re, random

FB_TOKEN = os.getenv("FB_PAGE_ACCESS_TOKEN")
FB_PAGE_ID = os.getenv("FB_PAGE_ID")
FB_APP_ID = os.getenv("FB_APP_ID")
FB_APP_SECRET = os.getenv("FB_APP_SECRET")
GROQ_KEY = os.getenv("GROQ_API_KEY")

MEMORY_COMMENTS = "replied_comments.json"
MEMORY_INBOX = "replied_inbox.json"
LEARN_FILE = "learned_topics.json"

BANNED = ["mjinga", "umbwa", "meffi"]
LEAK_PHRASES = ["Answer inside double quotes", "18-24 words", "1-2 emojis"]

def load_json(f):
    try: return json.load(open(f,"r"))
    except: return {}
def save_json(f,d):
    with open(f,"w") as x: json.dump(d,x)
def is_clean(t):
    if not t: return False
    low=t.lower()
    if any(w in low for w in BANNED): return False
    if any(p.lower() in low for p in LEAK_PHRASES): return False
    return True

def ask_groq(sys_prompt, user_prompt):
    MODELS = ["openai/gpt-oss-20b", "openai/gpt-oss-120b"]
    for model in MODELS:
        try:
            r=requests.post("https://api.groq.com/openai/v1/chat/completions",
                headers={"Authorization":f"Bearer {GROQ_KEY}","Content-Type":"application/json"},
                json={"model":model,"messages":[{"role":"system","content":sys_prompt},{"role":"user","content":user_prompt}],"max_tokens":300},
                timeout=40)
            j=r.json()
            print(f"TRY {model}: {str(j)[:700]}")
            if "choices" not in j:
                print(f"NO ACCESS {model}: {j.get('error')}")
                continue
            raw=j["choices"][0]["message"].get("content") or ""
            print(f"RAW: {raw[:400]}")
            m=re.search(r'"([^"]{10,400})"',raw,re.DOTALL)
            final=m.group(1).strip() if m else raw.strip()[:300]
            final=final.strip().rstrip("?").strip()
            if is_clean(final) and len(final)>15:
                print(f"SUCCESS {model}: {final}")
                return final
        except Exception as e:
            print(f"err {model}: {e}")
            continue
    return ""

def make_post():
    learned=json.load(open(LEARN_FILE)) if os.path.exists(LEARN_FILE) else []
    topics=["why kindness is revolutionary","how setting boundaries protects peace","self-worth when you feel behind","mental health its okay to not be okay","forgiveness as freedom","community healing checking on silent friends","words have power speak life","rest is productive","empathy listening to understand"]
    avail=[t for t in topics if t not in learned[-5:]]
    chosen=random.choice(avail or topics)
    learned.append(chosen)
    if len(learned)>30: learned.pop(0)
    open(LEARN_FILE,"w").write(json.dumps(learned))
    print(f"Researching: {chosen}")
    post=ask_groq("You are REAL Satisfaction AI. Warm mature healer. Answer inside double quotes only, 18-24 words, 1-2 emojis, ends with question.", f"Create Facebook post about {chosen}. Positive impact. Inside quotes.")
    print(f"FINAL POST: {post}")
    if post:
        final_fb=f"{post}? 🌱🤝 #RealSatisfaction #Healing"
        url=f"https://graph.facebook.com/v20.0/{FB_PAGE_ID}/feed"
        fb=requests.post(url,data={"message":final_fb,"access_token":FB_TOKEN}).json()
        print(f"FB: {fb}")

def reply_comments():
    mem=load_json(MEMORY_COMMENTS)
    if isinstance(mem,list): mem={k:True for k in mem}
    posts=requests.get(f"https://graph.facebook.com/v20.0/{FB_PAGE_ID}/posts",params={"access_token":FB_TOKEN,"limit":5}).json().get("data",[])
    new=False
    for p in posts:
        comms=requests.get(f"https://graph.facebook.com/v20.0/{p['id']}/comments",params={"access_token":FB_TOKEN,"fields":"id,message,from","limit":20}).json().get("data",[])
        for c in comms:
            cid=c["id"]
            if cid in mem: continue
            if c.get("from",{}).get("id")==FB_PAGE_ID: continue
            if not c.get("message"): continue
            txt="Asante sana for sharing. You help our community heal together?"
            requests.post(f"https://graph.facebook.com/v20.0/{cid}/comments",data={"message":txt,"access_token":FB_TOKEN})
            mem[cid]=datetime.datetime.now().isoformat()
            new=True
            print(f"Replied {cid}")
    if new: save_json(MEMORY_COMMENTS, mem)

def reply_inbox():
    mem=load_json(MEMORY_INBOX)
    convs=requests.get(f"https://graph.facebook.com/v20.0/{FB_PAGE_ID}/conversations",params={"access_token":FB_TOKEN,"fields":"id,messages{id,message,from}","limit":10}).json().get("data",[])
    new=False
    for conv in convs[:10]:
        msgs=conv.get("messages",{}).get("data",[])
        if not msgs: continue
        last=msgs[0]
        mid=last["id"]
        if mid in mem: continue
        if last.get("from",{}).get("id")==FB_PAGE_ID: continue
        rep="Asante kwa DM mkuu. Nimepokea message yako. Niko hapa kukusupport, poa poa?"
        requests.post(f"https://graph.facebook.com/v20.0/{conv['id']}/messages",data={"message":rep,"access_token":FB_TOKEN})
        mem[mid]=datetime.datetime.now().isoformat()
        new=True
        print(f"Inbox {mid}")
    if new: save_json(MEMORY_INBOX, mem)

now=datetime.datetime.now()
if now.hour%3==0 and now.minute<20:
    make_post()
else:
    print(f"Skip post hour {now.hour}")
reply_comments()
reply_inbox()
