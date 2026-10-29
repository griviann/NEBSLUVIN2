# bot.py — Kenya Me V3.1 — Full Handler — Real Satisfaction — Kenyan You
# Post + Comment Reply + Inbox DM + Leak-Proof + Memory + Token Auto-Refresh

import os, json, requests, datetime
from groq import Groq

FB_TOKEN = os.getenv("FB_PAGE_ACCESS_TOKEN")
FB_PAGE_ID = os.getenv("FB_PAGE_ID")
FB_APP_ID = os.getenv("FB_APP_ID")
FB_APP_SECRET = os.getenv("FB_APP_SECRET")
GROQ_KEY = os.getenv("GROQ_API_KEY")

MEMORY_COMMENTS = "replied_comments.json"
MEMORY_INBOX = "replied_inbox.json"

BANNED = ["mjinga", "umbwa", "meffi"]
LEAK_PHRASES = ["Answer inside double quotes", "18-24 words", "1-2 emojis"]

client = Groq(api_key=GROQ_KEY)

class KenyaMe:
    def check_leak(self, text):
        low = text.lower()
        if any(w in low for w in BANNED): return False
        if any(p.lower() in low for p in LEAK_PHRASES): return False
        return True
    def clean_post(self, raw):
        if not self.check_leak(raw): return None
        if '"' in raw:
            try: raw = raw.split('"')[1]
            except: pass
        return raw.strip().rstrip("?").strip()
    def reply_comment(self, msg):
        low = msg.lower()
        if "stress" in low or "choka" in low: return "Pole sana mkuu, niko hapa. Your quiet presence matters. Tuongee kidogo? 🌱"
        if "poa" in low or "sasa" in low: return "Poa sana mkuu, asante kwa ku-share. Pamoja tunaweza kupona pole pole?"
        return "Asante sana for sharing. You help our community heal together? 🤝"
    def reply_inbox(self, msg):
        low = msg.lower()
        if "help" in low or "msaada" in low: return "Niko hapa mkuu, karibu. Tell me more, niko na time yako. Pamoja tutapata solution?"
        if "stress" in low or "sad" in low: return "Nimesikia mkuu. It is okay to feel that way. You are not alone. Tuongee zaidi?"
        return "Asante kwa DM mkuu. Nimepokea message yako. Niko hapa kukusupport, poa poa?"

bot = KenyaMe()

def load_json(f):
    try:
        with open(f,"r") as x: return json.load(x)
    except: return {}
def save_json(f,d):
    with open(f,"w") as x: json.dump(d,x)

def refresh_token():
    try:
        debug_url = f"https://graph.facebook.com/debug_token?input_token={FB_TOKEN}&access_token={FB_APP_ID}|{FB_APP_SECRET}"
        info = requests.get(debug_url).json()
        expires = info.get("data",{}).get("expires_at",0)
        now = datetime.datetime.now().timestamp()
        if expires - now < 864000:
            print("TOKEN EXPIRING - REFRESHING...")
            exchange_url = f"https://graph.facebook.com/v18.0/oauth/access_token?grant_type=fb_exchange_token&client_id={FB_APP_ID}&client_secret={FB_APP_SECRET}&fb_exchange_token={FB_TOKEN}"
            new = requests.get(exchange_url).json().get("access_token")
            if new: print(f"NEW TOKEN: {new[:20]}... SAVE IN SECRETS!"); return new
    except Exception as e: print(f"Refresh skipped: {e}")
    return FB_TOKEN

def make_post():
    prompt = """Write 1 hopeful Kiswahili/English post for Kenyan youth, 18-24 words, 1-2 emojis, ends with question. Topics: healing, silent friends. Simple English. Answer inside double quotes only."""
    res = client.chat.completions.create(model="llama-3.3-70b-versatile", messages=[{"role":"user","content":prompt}], max_tokens=100)
    raw = res.choices[0].message.content
    clean = bot.clean_post(raw)
    if not clean: print(f"LEAK BLOCKED: {raw}"); return
    final = f"{clean}? 🌱 #RealSatisfaction"
    url = f"https://graph.facebook.com/{FB_PAGE_ID}/feed"
    r = requests.post(url, data={"message": final, "access_token": FB_TOKEN})
    print(f"POSTED: {final} | {r.text}")

def reply_comments():
    mem = load_json(MEMORY_COMMENTS)
    posts_url = f"https://graph.facebook.com/{FB_PAGE_ID}/posts?fields=id&limit=5&access_token={FB_TOKEN}"
    posts = requests.get(posts_url).json().get("data",[])
    new=False
    for p in posts:
        c_url = f"https://graph.facebook.com/{p['id']}/comments?fields=id,message,from&access_token={FB_TOKEN}"
        comms = requests.get(c_url).json().get("data",[])
        for c in comms:
            cid=c["id"]
            if cid in mem: continue
            if c.get("from",{}).get("id")==FB_PAGE_ID: continue
            txt=bot.reply_comment(c.get("message",""))
            requests.post(f"https://graph.facebook.com/{cid}/comments", data={"message":txt,"access_token":FB_TOKEN})
            mem[cid]=datetime.datetime.now().isoformat(); new=True; print(f"REPLIED COMMENT {cid}")
    if new: save_json(MEMORY_COMMENTS,mem)

def reply_inbox():
    mem=load_json(MEMORY_INBOX)
    url=f"https://graph.facebook.com/{FB_PAGE_ID}/conversations?fields=id,messages{{id,message,from}}&access_token={FB_TOKEN}"
    convos=requests.get(url).json().get("data",[])
    new=False
    for conv in convos[:10]:
        msgs=conv.get("messages",{}).get("data",[])
        if not msgs: continue
        last=msgs[0]
        mid=last["id"]
        if mid in mem: continue
        if last["from"]["id"]==FB_PAGE_ID: continue
        txt=bot.reply_inbox(last.get("message",""))
        requests.post(f"https://graph.facebook.com/{conv['id']}/messages", data={"message":txt,"access_token":FB_TOKEN})
        mem[mid]=datetime.datetime.now().isoformat(); new=True; print(f"REPLIED INBOX {mid}")
    if new: save_json(MEMORY_INBOX,mem)

refresh_token()
now=datetime.datetime.now()
if now.hour%3==0 and now.minute<20: make_post()
reply_comments()
reply_inbox()
