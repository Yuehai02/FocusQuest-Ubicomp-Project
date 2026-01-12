# FocusQuest 

### A Context-Aware AI Learning Companion & Gamified Focus Guardian

**FocusQuest** (formerly Lehigh StudyMate) is a mobile ubiquitous computing system designed to optimize the learning experience. Unlike traditional study tools that only manage schedules, FocusQuest proactively **senses** the user's physical environment, **infers** cognitive and emotional states, and **adapts** its interaction strategies to provide personalized guidance, health protection, and psychological support.

-----

##  Table of Contents

  - [Project Motivation](https://www.google.com/search?q=%23-project-motivation)
  - [Key Features](https://www.google.com/search?q=%23-key-features)
      - [1. Multi-Dimensional AI Tutor](https://www.google.com/search?q=%231-multi-dimensional-ai-tutor-reasoning)
      - [2. Super Focus Mode (Ubicomp Sensing)](https://www.google.com/search?q=%232-super-focus-mode-ubicomp-sensing)
      - [3. Gamified Productivity (RPG System)](https://www.google.com/search?q=%233-gamified-productivity-rpg-system)
      - [4. Context-Aware Emotion Care](https://www.google.com/search?q=%234-context-aware-emotion-care)
      - [5. Intelligent Utilities](https://www.google.com/search?q=%235-intelligent-utilities)
  - [Technical Architecture](https://www.google.com/search?q=%23-technical-architecture)
  - [Installation & Setup](https://www.google.com/search?q=%23-installation--setup)
  - [Privacy & Data Sovereignty](https://www.google.com/search?q=%23-privacy--data-sovereignty)
  - [Future Roadmap](https://www.google.com/search?q=%23-future-roadmap)

-----

## Project Motivation

Students often struggle with low efficiency, physical fatigue from poor posture, and academic anxiety. Existing apps are passive tools. **FocusQuest** aims to bridge the gap between the **physical world** (posture, noise, gestures) and the **digital learning space** (knowledge, planning) using on-device machine learning and Large Language Models (LLMs).

-----

##  Key Features

### 1\. Multi-Dimensional AI Tutor (Reasoning)

Moves beyond simple Q\&A by acting as a Socratic mentor.

  * **Step-by-Step Guidance:** Instead of giving answers, the AI guides users through **Cognitive** (definitions), **Structure** (steps), and **Verification** (edge cases) phases.
  * **Meta-Cognitive Check:** Detects if a user asks a repeated question and prompts them to recall the answer ("Hint" vs. "Answer") to reinforce memory.
  * **Context Preservation:** Automatically saves chat history locally for continuity.

### 2\. Super Focus Mode (Ubicomp Sensing)

A "multimodal OS" for deep work that monitors physical health in real-time.

  * **Distance Guardian:** Uses **ML Kit Face Mesh** to calculate screen distance. If the user gets too close (\>0.35 screen ratio) for 20s, a **blur overlay** activates to protect eyesight.
  * **Posture Coach:** Analyzes **Head Euler Angles** (Pitch/Roll/Yaw) via the front camera to detect slouching or harmful neck angles, providing immediate visual feedback (Red/Green indicators).
  * **Neck Stretch Exercises:** An interactive, computer-vision-guided gym. The app tracks head movements (Left -\> Right -\> Up -\> Down -\> Tilt) to guide users through relaxation exercises.
  * **Environment Awareness:** Uses the **Microphone (MediaRecorder)** to monitor ambient noise levels (dB) and warn users of distracting environments.
  * **Battery Optimization:** Implements a periodic sensing cycle (e.g., check 10s, sleep 60s) to minimize power consumption.

### 3\. Gamified Productivity (RPG System)

Transforms study time into an adventure to leverage "Loss Aversion" psychology.

  * **Focus to Attack:** Every minute of focus deals damage to the current "Boss" (e.g., *Procrastination Slime*, *Distraction Goblin*).
  * **Regeneration Penalty:** If the user stops studying for too long, the Boss heals its HP.
  * **Overkill Mechanic:** Excess damage dealt to a dying boss carries over to the next level, ensuring every minute counts.

### 4\. Context-Aware Emotion Care

  * **Sentiment Interception:** Analyzes user input locally for distress keywords (e.g., "hopeless", "tired").
  * **Adaptive Persona:**
      * *Mild Stress:* Suggests breathing exercises or Lofi music.
      * *Severe Distress:* Provides safety interventions and links to campus psychological resources (e.g., Lehigh UCPS).
  * **Smart Greetings:** Generates warm, context-aware opening messages based on previous conversation history (e.g., "Is your headache better today?").
  * **Bilingual Support:** One-tap toggle to switch the AI Therapist between English and Chinese for native-language emotional comfort.

### 5\. Intelligent Utilities

  * **Smart Lecture Summary:** Captures lecture notes via camera, uses **OCR (Text Recognition)** to extract text, and uses GPT to generate structured summaries and formula indexes.
  * **Head-Gesture Quiz:** A hands-free flashcard game. Nod (Pitch) for "Know", Shake (Yaw) for "Don't Know". Great for eating or multitasking.
  * **AI Planner:** Voice-activated task entry. The AI organizes tasks by priority and execution order.
  * **Cloud Sync:** Optional Firebase integration to backup habits, focus history, and user profile across devices.

-----

## Technical Architecture

### Sensors & Hardware

  * **CameraX:** Image analysis for posture tracking and distance estimation.
  * **Microphone:** Ambient noise decibel calculation.
  * **Accelerometer/Gyroscope:** Device motion detection (distraction monitoring).

### AI & Machine Learning

  * **Google ML Kit:** On-device, privacy-first Face Detection (Euler Angles, Bounding Box) and Text Recognition (OCR).
  * **OpenAI API (GPT-3.5-turbo):** Natural language processing for tutoring, summarization, and emotional support.
  * **Retrofit + OkHttp:** Network layer for API communication.

### Backend & Data

  * **Firebase Authentication:** Secure user login/registration.
  * **Firebase Firestore:** Cloud storage for leaderboards and encrypted data backups.
  * **SharedPreferences + Gson:** Local persistence for chat history, habits, and RPG game state.

### UI/UX

  * **Custom Views:** Hand-written `PieChartView` and `SimpleLineChart` for data visualization.
  * **Reactive UI:** Dynamic layout changes (View Flipping) in Super Focus Mode.

-----

## Installation & Setup

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/YourUsername/FocusQuest.git
    ```
2.  **Configure OpenAI API Key:**
      * Create a `local.properties` file in the root directory (if not present).
      * Add your key: `OPENAI_API_KEY="sk-your-actual-key-here"`
3.  **Configure Firebase:**
      * Create a project in the [Firebase Console](https://console.firebase.google.com/).
      * Add an Android App with package name `com.example.lehighstudymate`.
      * Download `google-services.json` and place it in the `app/` directory.
4.  **Build & Run:**
      * Open in Android Studio (Hedgehog or later recommended).
      * Sync Gradle.
      * Run on an Emulator (ensure Camera is set to "Webcam0") or a Physical Device (recommended for sensors).

-----

## Privacy & Data Sovereignty

  * **On-Device Processing:** All camera and microphone data for posture/noise detection is processed instantly on the device and **never** uploaded to any server.
  * **Local-First Storage:** Chat logs and detailed focus records are stored locally by default.
  * **Optional Cloud Sync:** Users must explicitly opt-in to upload backups or join the public leaderboard.
  * **Data Wipe:** A "Log Out & Clear Data" feature ensures all local traces are removed upon request.

-----

## Future Roadmap

  * **TTS (Text-to-Speech):** Read lecture summaries aloud for auditory learning.
  * **Calendar Integration:** Export AI-generated plans directly to Google Calendar.
  * **LBS (Location-Based Services):** Automatically suggest "Deep Focus" when entering the library.
  * **On-Device LLM:** Transition to Gemini Nano for offline, zero-latency AI interaction.

-----

**Developed by Yuehai Yang**
*Graduate Project for Ubiquitous Computing*
