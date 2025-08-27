<p align="center">
  <img src="https://img.shields.io/badge/Java-11+-red?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/CLI-App-blue?style=for-the-badge&logo=gnu-bash&logoColor=white" />
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" />
</p>

<h1 align="center">📋 Simple Java To-Do App (CLI)</h1>

<p align="center">
  A minimal, fast, and persistent <b>command-line To-Do manager</b> written in pure Java.  
  <br>No frameworks, no GUIs, just clean Java 11+ code.
</p>

---

## ✨ Features
- ➕ Add tasks with **optional due date, priority, and tags**
- 📋 List tasks (all, open, done) with sorting options
- ✅ Mark tasks as **done/undone**
- 📝 Edit tasks inline (title, due date, tags, priority)
- ❌ Delete tasks or clear all completed ones
- 🔍 Search by title or tags
- 💾 Persistent storage in a simple `tasks.tsv` file

---

## 🚀 Getting Started

### 1. Compile
```bash
javac TodoApp.java
```
### 2. Run
```bash
java TodoApp
```

### ===== Simple CLI To-Do =====
Storage: tasks.tsv

Commands:
  add <title> [/due yyyy-mm-dd] [/p low|med|high] [/tags tag1,tag2]
  list [all|open|done] [/sort id|due|prio] [/rev]
  done <id>      — mark complete
  undone <id>    — mark incomplete
  edit <id> [/t new title] [/due yyyy-mm-dd|none] [/p low|med|high] [/tags list|none]
  del <id>       — delete task
  search <text>  — find in titles or tags
  clear          — remove all completed tasks
  save           — persist now (auto-saves on exit)
  exit           — save & quit

> add Buy milk /due 2025-08-30 /p high /tags shopping,urgent
Added #1: Buy milk

> list all
ID   ✔  Due        Prio   Title                 Tags
-------------------------------------------------------------
1       2025-08-30 HIGH   Buy milk              shopping,urgent

### Storage Format
tasks are stored in a simple TSV file tasks.tsv for easy reading/editing:
```shell
# id   done   priority   due        title         tags
1      0      HIGH       2025-08-30 Buy milk      shopping,urgent
```
### 🔧 Tech Stack

- Java 11+

- Pure CLI (no GUI, no Maven)

- Data persistence via plain text files

### 🤝 Contributing

- Fork this repo 🍴

- Create your feature branch 🔨
```bash
git checkout -b feature/new-feature
```


- Commit changes 🎉
```bash
git commit -m "Add new feature"
```

- Push branch 🚀
```bash
git push origin feature/new-feature
```

- Open a Pull Request ✅

### 📜 License

This project is licensed under the MIT License – feel free to use, modify, and share.

<p align="center"> 💡 “Organize today, achieve tomorrow.” </p>

