# RealSatisfaction AI

**Location & Focus Audience:** Kisumu, Kenya  
**Target Platform:** Android (Jetpack Compose, Kotlin) & Meta Facebook Graph API Integration  
**Languages Supported:** English and Kiswahili  

---

## 1. Overview

**RealSatisfaction AI** is an AI social media management application built for administrators in Kisumu, Kenya, to manage an authorized Facebook Page. It provides a prompt-driven chat interface alongside dedicated sections for content approval, comment moderation, scheduling, and system audits.

### Core Architecture
- **Language & Framework:** Kotlin, Jetpack Compose, Material Design 3
- **Local Persistence:** Room Database (SQLite) for posts, drafts, comments, audit trail, and settings
- **AI Command Engine:** Groq API (`llama-3.3-70b-versatile` or custom) with localized Kisumu Kiswahili/English fallback engine
- **Social Media API:** Official Meta Facebook Graph API (`v20.0`+)
- **Scheduler:** GitHub Actions companion workflow (`.github/workflows/bot.yml`) configured for 3-hour execution intervals aligned with UTC and East Africa Time (EAT, UTC+3)

---

## 2. Dashboard Sections

1. **AI Chat Box:** Multi-line text instruction engine that interprets English and Kiswahili commands, extracts parameters, evaluates moderation, and executes backend actions with live status feedback.
2. **Content Hub:** Review, edit, approve, reject, and publish post drafts. Includes live character counter (<= 280 chars), sentence counter (<= 2 sentences), and moderation diagnostics.
3. **Comments & Replies:** Manage incoming Facebook comments. Automatically categorizes comments (Positive Feedback, General Question, Complaint, Business Inquiry, Spam, Sensitive Issue, Needs Human Attention) and drafts respectful responses for administrator approval before sending.
4. **Schedule & Automation:** Interval selector (3h, 6h, 12h, 24h), UTC and East Africa Time (EAT) display, big Pause/Resume toggle, and GitHub Actions cron integration.
5. **Settings & Health Check:** Real-time diagnostics for Meta Facebook connectivity, Groq AI connectivity, content pillars management, blocked-word list manager, and audit log inspection.

---

## 3. Supported Chat Commands

The AI understands ordinary instructions in both English and Kiswahili:

- **Create a post:**  
  *“Create a positive Kiswahili post about small businesses in Kisumu.”*  
  *“Tengeneza chapisho kuhusu biashara ndogo Kisumu.”*
- **Prepare post with approval gate:**  
  *“Prepare a post about protecting Lake Victoria and show it to me before publishing.”*  
  *“Andaa chapisho kuhusu kulinda Ziwa Victoria na unionyeshe kabla ya kuchapisha.”*
- **Check comments:**  
  *“Show me new comments on our Facebook Page.”*  
  *“Nionyeshe maoni mapya kwenye Ukurasa wa Facebook.”*
- **Draft reply:**  
  *“Draft a respectful response to this comment.”*  
  *“Andika jibu kwa heshima kwa maoni haya.”*
- **Publish:**  
  *“Publish the approved post.”*  
  *“Chapisha chapisho lililoidhinishwa.”*
- **Pause/Resume:**  
  *“Pause automatic posting until tomorrow.”* / *“Sitisha machapisho ya kiotomatiki.”*  
  *“Resume automatic posting.”* / *“Endeleza machapisho.”*
- **Schedule:**  
  *“Change the schedule to every six hours.”* / *“Badilisha ratiba iwe kila masaa sita.”*
- **Add content topics:**  
  *“Add youth employment to our content topics.”* / *“Ongeza ajira ya vijana kwenye mada.”*
- **Health check & audits:**  
  *“Run a system health check.”* / *“Kagua hali ya mfumo.”*  
  *“Show me the last ten actions performed by the bot.”* / *“Nionyeshe vitendo kumi vya mwisho.”*

---

## 4. The Six Content Pillars

1. **Biashara Ndogo:** Small businesses, customer service, bookkeeping, and entrepreneurship in Kisumu.
2. **Mazingira na Ziwa Victoria:** Conservation, beach cleanups, plastic waste reduction, and lake protection.
3. **Afya na Lishe:** Preventative wellness, traditional nutrition, and community health.
4. **Elimu na Vijana:** Youth employment, vocational education, and digital skills development.
5. **Utalii Kisumu:** Dunga Beach, Impala Sanctuary, cultural heritage, and lake attractions.
6. **Kilimo Kisumu:** Modern cage fish farming, drip irrigation, and agribusiness.

---

## 5. Content Moderation & Quality Control

Before any post is saved or published, it passes through the server-side moderation pipeline:
- **Blocked words list:** Seeded with `mjinga`, `punda`, `matako`, `umbwa` (customizable via Settings).
- **Format limits:** Up to 280 characters, maximum 2 sentences, maximum 2 hashtags.
- **Privacy filters:** Automatic redaction and blocking of phone numbers and email addresses.
- **Duplicate prevention:** Jaccard text similarity comparison against recent posts.

---

## 6. Meta Facebook Graph API Setup

To connect your real Facebook Page:
1. Obtain a **Page Access Token** with permissions:
   - `pages_show_list`
   - `pages_read_engagement`
   - `pages_manage_posts`
   - `pages_read_user_content`
   - `pages_manage_engagement`
2. Enter your **Page ID** and **Page Access Token** in the app's **Settings** screen (or set `FB_PAGE_ID` and `FB_PAGE_ACCESS_TOKEN` in the Secrets panel / `.env`).
3. Tap **Hifadhi na Jaribu Uunganisho (Save & Test)**.
