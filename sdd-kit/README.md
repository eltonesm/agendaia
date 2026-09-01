# SDD Kit — Spec-Driven Development for Teams

A methodology framework that helps developers build software predictably using AI coding assistants (Claude Code, Cursor, etc.).

> Built for communities learning software development. Teach your team to build with confidence — specs first, code second.

---

## What is Spec-Driven Development?

SDD is a structured approach where every feature goes through 4 phases before a single line of code is written:

```
/sdd.spec  →  /sdd.plan  →  /sdd.build  →  /sdd.finish
  (WHAT)        (TASKS)       (CODE)        (DONE)
```

This ensures:
- The team agrees on **what** to build before discussing **how**
- Implementation tasks are clear and reviewable
- Quality gates run automatically at every step

---

## Quick Start (3 steps)

> **Read [`PORTABILITY.md`](PORTABILITY.md) once before your first feature.**
> It's a ground-truth audit — what actually worked, what was broken and got
> fixed, and what got removed — from running this kit's full cycle twice on
> a real project. Saves you from rediscovering the same three bugs by hand.

### 1. Install

```bash
# Clone or copy this directory into your project
# Then run the installer:
bash sdd-kit/install.sh
```

### 2. Configure

Open Claude Code in your project and run:
```
/sdd.project
```

Fill in your project conventions (team name, tech stack, branch strategy).

### 3. Start your first feature

**Express mode** (recommended for beginners):
```
/sdd.go "add user login screen"
```

**Standard mode** (full control):
```
/sdd.start "add user login screen"
/sdd.spec
/sdd.plan
/sdd.build
/sdd.finish
```

---

## Commands Reference

| Command | Description |
|---------|-------------|
| `/sdd.go` | Express mode — full workflow in one command |
| `/sdd.start` | Initialize a new feature |
| `/sdd.spec` | Create functional and technical specifications |
| `/sdd.plan` | Generate implementation task list |
| `/sdd.build` | Implement tasks with quality gates |
| `/sdd.finish` | Validate and archive feature |
| `/sdd.check` | Check feature status |
| `/sdd.fix` | Fix spec/code inconsistencies |
| `/sdd.list` | List all features |
| `/sdd.backlog` | Manage TODO/DEBT/IDEA items |
| `/sdd.hub` | Coordinate multi-app features |
| `/sdd.import` | Import existing specs |
| `/sdd.reverse-eng` | Generate specs from existing code |
| `/sdd.project` | Manage project configuration |
| `/sdd.cancel` | Cancel current feature |
| `/sdd.rollback` | Roll back to a previous phase |
| `/sdd.doctor` | Diagnose configuration issues |
| `/sdd.help` | Show help |

---

## Project Structure

After installation, your project will have:

```
your-project/
├── .claude/
│   ├── commands/        # sdd.* slash commands
│   ├── skills/          # AI skills for code review, validation, etc.
│   └── agents/          # Specialized AI subagents
├── sdd/
│   ├── wip/             # Features in progress
│   └── features/        # Completed features (archived)
└── CLAUDE.md            # AI assistant configuration
```

---

## Manual Installation (without install.sh)

Copy the agentic files to your project's `.claude/` directory:

```bash
cp -r sdd-kit/.claude/commands/  .claude/commands/
cp -r sdd-kit/.claude/skills/    .claude/skills/
cp -r sdd-kit/.claude/agents/    .claude/agents/
```

Then add to your project's `CLAUDE.md`:
```
@sdd-kit/CLAUDE.md
```

---

## Requirements

- [Claude Code](https://claude.ai/code) (CLI or IDE extension)
- Any project in any language (Java, Python, Node, Go, etc.)
- No additional tools required — works with Claude Code out of the box

---

## Framework Structure

```
sdd-kit/
├── CLAUDE.md              # AI entry point
├── README.md              # This file
├── install.sh             # Installer script
├── .claude/
│   ├── commands/          # 18 slash commands
│   ├── skills/            # 7 AI skills
│   └── agents/            # 10 specialized agents
└── framework/
    ├── WORKFLOW.md        # Complete workflow guide
    ├── COMMANDS.md        # Command reference
    ├── MODES.md           # Express vs Standard modes
    ├── GLOSSARY.md        # SDD terminology
    ├── QUICK_REFERENCE.md # Cheat sheet
    ├── templates/         # Spec and task templates
    ├── standards/         # Coding and process standards
    └── tools/             # Helper shell scripts
```

---

## Contributing

This kit is designed to be extended. To add support for your tech stack:

1. Create a new skill in `.claude/skills/your-stack-expert/SKILL.md`
2. Reference it in `CLAUDE.md` under "Skills Available"
3. Share with the community!

---

## License

MIT — free to use, fork, and teach.
