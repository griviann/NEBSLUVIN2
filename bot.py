cat > /tmp/bot.py << 'PY'
import os, requests, random, re

groq_key = os.getenv("GROQ_API_KEY")
page_token = os.getenv("FB_PAGE_ACCESS_TOKEN")
page_id = os.getenv("FB_PAGE_ID")
version = os.getenv("FACEBOOK_API_VERSION", "v20.0")
action = os.getenv("ACTION", "post")

def get_clean_post(topic):
    prompt = f"""You are a viral Facebook writer for REAL satisfaction page.
TOPIC: {topic}
RULES:
- Output ONLY the post itself. No explanation.
- Under 35 words.
- Emotional, relatable.
- 2 emojis max.
- 2 hashtags at end.
- Never write "We need to produce" or "Let's craft" or "Here is a post".
"""
    r = requests.post("https://api.groq.com/openai/v1/chat/completions",
        headers={"Authorization": f"Bearer {groq_key}", "Content-Type": "application/json"},
        json={
            "model": "openai/gpt-oss-20b",
            "messages": [
                {"role": "system", "content": "You output only the final Facebook post. No thinking, no intro."},
                {"role": "user", "content": prompt}
            ],
            "max_tokens": 150,
            "temperature": 0.8,
        }, timeout=30)
    
    data = r.json()
    msg = (data.get("choices") or [{}])[0].get("message") or {}
    # For gpt-oss-20b, REAL answer is in content, reasoning is trash
    content = (msg.get("content") or "").strip()
    reasoning = (msg.get("reasoning") or msg.get("reasoning_content") or "").strip()
    
    # Prefer content over reasoning
    final = content if content else reasoning
    
    # CLEAN reasoning leak
    final = re.sub(r'(?i)we need to produce.*?:', '', final)
    final = re.sub(r'(?i)let\'s craft:?', '', final)
    final = re.sub(r'(?i)here is.*?:', '', final)
    final = re.sub(r'(?i)under 40 words.*?\.', '', final)
    final = final.strip().strip('"').strip("'")
    
    # If still contains prompt words, take last 2 sentences
    if "viral Facebook post about" in final.lower():
        parts = final.split(".")
        final = ". ".join(parts[-3:]).strip()
    
    return final

# Generate
topic = random.choice(["customer satisfaction", "self love", "real love", "healing", "loyalty"])
post_text = get_clean_post(topic)
print(f"FINAL POST: {post_text}")

if len(post_text) < 10 or "we need to" in post_text.lower():
    print("Post still dirty, aborting to avoid spam")
else:
    pr = requests.post(f"https://graph.facebook.com/{version}/{page_id}/feed",
        data={"message": post_text, "access_token": page_token}, timeout=25)
    print(f"FB RESPONSE: {pr.json()}")
PY
