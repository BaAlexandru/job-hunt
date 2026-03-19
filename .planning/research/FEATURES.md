# Feature Research

**Domain:** Job Application Tracking (Personal Web App)
**Researched:** 2026-03-19
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Application CRUD with status | Core purpose of the app; every competitor has this | LOW | Statuses: Interested, Applied, Phone Screen, Interview, Offer, Rejected, Accepted, Withdrawn |
| Kanban board view | Every major tracker (Huntr, Teal, Eztrackr, Seekario) uses kanban as primary UI. Users expect drag-and-drop status changes | MEDIUM | Drag-and-drop between status columns. This is how users mentally model their pipeline |
| List/table view | Some users prefer dense tabular data; Huntr, Teal, Built In all offer dual views | LOW | Sortable, filterable table. Complement to kanban, not replacement |
| Company management | Users track multiple roles at the same company; need company-level data (name, website, notes, location) | LOW | Link multiple jobs to one company. Store company metadata |
| Job posting details | Users need to store job title, description, URL, salary range, location, job type | LOW | Manual entry first. Job description text is critical for AI features later |
| Application date tracking | Users need to know when they applied, when they last heard back, interview dates | LOW | Created date, applied date, last activity date, next action date |
| Notes per application | Users jot down interview prep, recruiter names, follow-up reminders | LOW | Free-text notes field on each application. Simple but essential |
| Document upload and linking | Users need to know which CV and cover letter they sent for each application. Huntr, Eztrackr, Seekario all support this | MEDIUM | Upload PDF/DOCX, link docs to specific applications. Store per-job versions |
| Search and filtering | With 50+ applications, finding specific ones is critical | LOW | Filter by status, company, date range. Full-text search on job title and company |
| User authentication | Personal data requires login protection even for single-user | MEDIUM | JWT-based auth. Register/login flow. Protects sensitive job search data |
| Responsive design | Users check their tracker on phones between interviews | LOW | Not a native app, but the web app must be usable on mobile viewports |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| AI-powered CV optimization | Analyze job description + existing CV and suggest targeted adjustments. Huntr charges $40/mo for this. Building it yourself = free unlimited usage | HIGH | Requires: stored job description + stored CV + AI provider integration. Deferred to later phase per PROJECT.md |
| AI cover letter generation | Generate tailored cover letters from job description + CV + company info. Major paid feature in Huntr/Teal | HIGH | Same prerequisites as CV optimization. Deferred to later phase |
| Application analytics dashboard | Visualize application funnel: how many applied vs interviews vs offers. Response rate by company type. Most free trackers lack this | MEDIUM | Aggregate queries on application data. Charts showing conversion rates, activity over time, status distribution |
| Contact/networking tracker | Link recruiter names, emails, LinkedIn profiles to applications. Track who you talked to and when. Huntr Pro feature | MEDIUM | Separate contacts entity linked to applications. Not MVP but high value for active networkers |
| Reminders and follow-ups | Alert when no response after X days. Remind to send thank-you notes. JobHero has this | MEDIUM | Needs next-action-date field + notification mechanism (email or in-app) |
| Tags and custom labels | Categorize applications by tech stack, remote/onsite, contract type, priority level | LOW | Flexible tagging system. Useful for filtering and organization |
| Salary tracking and comparison | Record offered salary, expected range, and compare across applications | LOW | Fields on job/application. Simple but useful for negotiation decisions |
| Timeline/activity log | Chronological log of all events per application (applied, email received, interview scheduled) | MEDIUM | Event-sourced activity history. Richer than just status changes |
| Browser extension for job saving | One-click save from job boards (LinkedIn, Indeed, etc). Huntr and Teal both have popular extensions | HIGH | Chrome extension is a separate codebase. High effort, high convenience. Defer significantly |
| Job description URL scraping | Auto-extract job details from a posted URL instead of manual entry | MEDIUM | Web scraping is fragile (sites change layouts). PROJECT.md already marks this as v2 |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Job board integrations (LinkedIn, Indeed APIs) | "Auto-import my applications" | APIs are restricted/nonexistent for job seekers. LinkedIn aggressively blocks scraping. Indeed has no public job-seeker API. Maintenance burden is enormous | Manual entry + optional URL scraping for job details. Browser extension later |
| Real-time notifications/push | "Alert me when something changes" | Single-user app with self-entered data means nothing changes without the user's action. Push notification infra is complex for near-zero value | Simple reminder system: "follow up on applications with no activity in X days" |
| In-app document editor | "Edit my CV right in the app" | Building a document editor (especially for PDF/DOCX fidelity) is a massive effort. Google Docs/Word already exist | Upload-only approach. Edit externally, upload the result. AI suggests changes as text, user applies them in their editor |
| Collaborative/sharing features | "Share my job search with my mentor" | Adds multi-user complexity (permissions, sharing links, privacy). Distracts from core single-user value | Single-user focus. Export/share via PDF or link if needed later |
| Email integration (parse inbox) | "Auto-detect application confirmations from email" | Email parsing is unreliable, requires OAuth + provider-specific handling, privacy concerns with email access | Manual status updates. Activity log for tracking communications |
| Gamification (streaks, achievements) | "Motivate me to apply more" | Job searching is stressful; gamification can feel patronizing. Adds complexity without solving the core problem | Clean analytics showing activity trends is motivating without being gimmicky |
| Mobile native app | "I want it on my phone" | Two codebases to maintain. React Native or Flutter adds significant complexity | Responsive web app works on mobile. PWA (progressive web app) if needed later |

## Feature Dependencies

```
[User Authentication]
    └──requires──> (nothing, foundational)

[Company Management]
    └──requires──> [User Authentication]

[Job Posting Management]
    └──requires──> [Company Management]

[Application Tracking + Status Flow]
    └──requires──> [Job Posting Management]

[Kanban Board View]
    └──requires──> [Application Tracking + Status Flow]

[List/Table View]
    └──requires──> [Application Tracking + Status Flow]

[Document Upload]
    └──requires──> [User Authentication]

[Document-Application Linking]
    └──requires──> [Document Upload]
    └──requires──> [Application Tracking + Status Flow]

[AI CV Optimization]
    └──requires──> [Document Upload] (needs stored CV)
    └──requires──> [Job Posting Management] (needs job description)

[AI Cover Letter Generation]
    └──requires──> [Document Upload] (needs stored CV for context)
    └──requires──> [Job Posting Management] (needs job description)
    └──requires──> [Company Management] (needs company context)

[Application Analytics]
    └──requires──> [Application Tracking + Status Flow] (needs data to analyze)

[Contact/Networking Tracker]
    └──enhances──> [Application Tracking + Status Flow]
    └──enhances──> [Company Management]

[Tags and Labels]
    └──enhances──> [Application Tracking + Status Flow]
    └──enhances──> [Job Posting Management]

[Reminders]
    └──requires──> [Application Tracking + Status Flow]

[URL Scraping]
    └──enhances──> [Job Posting Management]
```

### Dependency Notes

- **AI features require both documents and job data:** This is why AI is correctly deferred. The document and job management systems must be solid first.
- **Kanban and list views are parallel:** Both depend on application tracking but not on each other. Can be built in either order, though kanban is the higher-value view.
- **Analytics requires accumulated data:** Building analytics too early means empty dashboards. Better to add after users have been tracking for a while.
- **Contact tracker is independent:** Can be added at any point after core application tracking exists.

## MVP Definition

### Launch With (v1)

Minimum viable product -- what's needed to start using the app for an actual job search.

- [ ] User authentication (register/login) -- protects personal data
- [ ] Company CRUD (name, website, location, notes) -- foundation for job tracking
- [ ] Job posting CRUD (title, description, URL, salary range, company link) -- what you're applying to
- [ ] Application tracking with status flow (drag-and-drop kanban + list view) -- the core loop
- [ ] Application dates (applied date, last activity, next action) -- timeline awareness
- [ ] Notes per application -- capture context and prep
- [ ] Document upload (PDF/DOCX) -- store CVs and cover letters
- [ ] Link documents to applications -- know which CV you sent where
- [ ] Basic search and filtering -- find applications by status, company, text

### Add After Validation (v1.x)

Features to add once core tracking is working and being used daily.

- [ ] Tags and custom labels -- once you have 20+ applications, categorization becomes essential
- [ ] Salary tracking fields -- useful during offer comparison phase
- [ ] Application analytics dashboard -- meaningful only after accumulating data
- [ ] Timeline/activity log per application -- richer history than just status changes
- [ ] Reminders for follow-ups -- "no response in 7 days" nudges
- [ ] Contact/networking tracker -- link people to applications and companies

### Future Consideration (v2+)

Features to defer until core product is proven useful.

- [ ] AI CV optimization -- requires stable document and job management. HIGH complexity
- [ ] AI cover letter generation -- same prerequisites as CV optimization
- [ ] Job posting URL scraping -- fragile, version-dependent, but convenient
- [ ] Browser extension -- separate codebase, high effort, high reward if app is used daily
- [ ] Export/reporting (PDF summary of job search) -- nice for review but not core

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Application tracking + status flow | HIGH | MEDIUM | P1 |
| Kanban board view | HIGH | MEDIUM | P1 |
| List/table view | HIGH | LOW | P1 |
| Company management | HIGH | LOW | P1 |
| Job posting management | HIGH | LOW | P1 |
| Document upload + linking | HIGH | MEDIUM | P1 |
| User authentication | HIGH | MEDIUM | P1 |
| Notes per application | MEDIUM | LOW | P1 |
| Search and filtering | MEDIUM | LOW | P1 |
| Application date tracking | MEDIUM | LOW | P1 |
| Tags and labels | MEDIUM | LOW | P2 |
| Salary tracking | MEDIUM | LOW | P2 |
| Analytics dashboard | MEDIUM | MEDIUM | P2 |
| Activity timeline | MEDIUM | MEDIUM | P2 |
| Reminders/follow-ups | MEDIUM | MEDIUM | P2 |
| Contact tracker | MEDIUM | MEDIUM | P2 |
| AI CV optimization | HIGH | HIGH | P3 |
| AI cover letter generation | HIGH | HIGH | P3 |
| URL scraping | MEDIUM | MEDIUM | P3 |
| Browser extension | MEDIUM | HIGH | P3 |

**Priority key:**
- P1: Must have for launch -- the app is useless without these
- P2: Should have, add when core is stable -- these make the app genuinely good
- P3: Nice to have, future consideration -- high effort or requires foundation to be solid first

## Competitor Feature Analysis

| Feature | Huntr (Free) | Huntr (Pro $40/mo) | Teal | Our Approach |
|---------|-------------|-------------------|------|--------------|
| Kanban board | Yes (100 app limit) | Yes (unlimited) | Yes | Yes, unlimited (self-hosted) |
| List view | Yes | Yes | Yes | Yes |
| Document storage | Basic | Full + per-job versions | Limited | Full, linked per application |
| AI resume tailoring | Basic scoring | Full AI generation | Yes (paid) | Phase 2+ with Spring AI |
| AI cover letters | No | Yes | Yes (paid) | Phase 2+ with Spring AI |
| Contact management | No | Yes | No | Phase 1.x |
| Analytics | No | Basic | Yes | Phase 1.x |
| Chrome extension | Yes | Yes | Yes | Phase 2+ (if ever) |
| Job board integration | No | No | LinkedIn save | No (manual entry) |
| Salary tracking | No | Yes | No | Phase 1.x |
| Reminders | No | No | No | Phase 1.x (differentiator) |
| Self-hosted/private | No (SaaS) | No (SaaS) | No (SaaS) | Yes (key advantage) |

**Key competitive insight:** The self-hosted nature is itself a differentiator. All major competitors are SaaS products with free tier limitations and paid AI features. Building your own means: unlimited applications, unlimited AI usage (you pay the API directly), full data ownership, and no monthly subscription.

## Sources

- [ApplyArc - Best Job Application Trackers](https://applyarc.com/blog/best-job-application-trackers)
- [ApplyArc - Comparison Guide](https://applyarc.com/compare/best-job-application-trackers)
- [Huntr - Product Page](https://huntr.co/product/job-tracker)
- [Huntr - Pricing](https://huntr.co/pricing)
- [Teal - Job Tracker](https://www.tealhq.com/tools/job-tracker)
- [Eztrackr](https://www.eztrackr.app/)
- [Seekario - Job Tracker](https://seekario.ai/job-tracker)
- [Sprout Blog - Best Trackers 2025](https://www.usesprout.com/blog/best-job-application-trackers)
- [Eztrackr Blog - 12 Best Trackers](https://www.eztrackr.app/blog/job-application-tracker)
- [Built In Job Tracker](https://builtin.com/articles/built-in-job-tracker-kanban-list)
- [Medium - Top 10 Trackers](https://medium.com/@avgenakisg/the-top-10-job-application-trackers-741ad5786ad5)

---
*Feature research for: Job Application Tracking*
*Researched: 2026-03-19*
