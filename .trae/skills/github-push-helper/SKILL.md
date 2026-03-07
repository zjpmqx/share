---
name: "github-push-helper"
description: "Safely checks local Git status, collects a commit message, commits, and pushes to a chosen branch. Invoke when the user wants to push local code to GitHub."
---

# GitHub Push Helper

## Purpose

Use this skill when the user wants to push local project changes to a GitHub repository from the current workspace. This skill guides the full flow: inspect repository state, confirm the target branch, collect a commit message, commit tracked changes, push to the selected remote branch, and clearly report success or failure.

This skill must prioritize safety, transparency, and good user experience. It must never expose credentials, tokens, SSH private keys, or full remote URLs containing secrets.

## Core responsibilities

1. Detect local Git repository status.
2. Show untracked, modified, deleted, and staged files in a readable summary.
3. Let the user provide a commit message through an interactive question.
4. Let the user choose the push target branch.
5. Run `git add`, `git commit`, and `git push` in the correct order.
6. Handle common failure cases with actionable, human-readable guidance.
7. Provide progress updates and final success or failure state.

## When to invoke

Invoke this skill when:

- the user asks to push local code to GitHub
- the user asks to commit and push current changes
- the user wants help publishing a branch to a remote repository
- the user wants a safe guided workflow for `git add`, `git commit`, and `git push`

Do not invoke this skill for code review, repository creation, or non-GitHub deployment tasks unless the user explicitly wants a Git push workflow.

## Required workflow

### 1. Verify Git context

First confirm that the working directory is inside a Git repository.

Recommended commands:

```powershell
git rev-parse --is-inside-work-tree
git branch --show-current
git remote -v
```

If not in a Git repository:

- stop the flow
- tell the user the current folder is not a Git repository
- suggest opening the correct project root or initializing Git if that is the intent

If no remote is configured:

- stop before push
- explain that no remote repository is configured
- suggest checking `git remote add origin <repo-url>`
- never invent or auto-configure a remote unless the user explicitly asks

### 2. Inspect repository status

Collect a concise status summary.

Recommended commands:

```powershell
git status --short
git status --branch
```

Report clearly:

- untracked files
- modified files
- deleted files
- staged files
- current branch
- whether the branch tracks a remote branch

If there are no changes to commit:

- tell the user there is nothing new to commit
- optionally allow push-only behavior if the branch is ahead of remote
- otherwise stop with a clear explanation

### 3. Ask for branch and commit message

Use an interactive question flow.

Branch selection requirements:

- detect current branch first
- if possible, list useful candidate branches, including current branch and local branches
- recommend the current branch by default
- let the user choose or provide another branch name

Commit message requirements:

- ask the user for a commit message before committing
- reject empty commit messages
- if the user provides a vague message like `update`, encourage a clearer message but still allow it if the user confirms

### 4. Confirm the operation

Before running mutating commands, summarize:

- repository path
- current branch
- target branch
- whether all changes or only current tracked changes will be staged
- commit message

Then ask for confirmation.

### 5. Execute Git commands safely

Typical sequence:

```powershell
git add -A
git commit -m "<message>"
git push origin <target-branch>
```

Execution rules:

- prefer `git add -A` when the user wants all current changes included
- if the repository already has staged changes, mention that to the user before commit
- if there is nothing to commit after staging, explain that the working tree is clean
- if the local branch differs from the target branch, use explicit refspec only when needed and explain what is being pushed

Examples:

```powershell
git push -u origin feature/my-branch
git push origin HEAD:feature/my-branch
```

Do not:

- print tokens, passwords, authorization headers, or embedded credentials
- echo full remote URLs if they may contain secrets
- run force-push unless the user explicitly requests it
- change Git config globally unless the user explicitly asks

## Error handling requirements

### Merge or non-fast-forward rejection

Common symptom examples:

- `[rejected] ... non-fast-forward`
- `failed to push some refs`

Response requirements:

- explain that the remote branch contains changes not present locally
- recommend pulling and rebasing or merging before pushing
- suggest safe next commands such as:

```powershell
git pull --rebase origin <branch>
```

- do not force-push automatically

### Merge conflicts during pull/rebase

Response requirements:

- explain that Git requires manual conflict resolution
- identify conflicted files if available
- tell the user to resolve conflicts, stage the resolved files, and continue rebase or commit the merge

### Network failures

Common symptom examples:

- timeout
- could not resolve host
- connection reset

Response requirements:

- explain that the network connection to the remote failed
- suggest retrying after checking VPN, proxy, DNS, or connectivity
- distinguish network failure from authentication failure where possible

### Authentication or permission failures

Common symptom examples:

- `Permission denied (publickey)`
- `Authentication failed`
- `403`

Response requirements:

- explain that GitHub rejected authentication or the user lacks repository access
- suggest checking SSH keys, GitHub login state, personal access token setup, or repository permissions
- never ask the user to paste secrets into chat unless absolutely necessary, and prefer secure local setup guidance instead

### Missing upstream branch

Response requirements:

- explain that the current branch has no upstream tracking branch
- suggest using:

```powershell
git push -u origin <branch>
```

### Empty commit / nothing to commit

Response requirements:

- explain that no new changes were detected for commit
- if local commits already exist and only push is needed, communicate that separately

## UX requirements

- Provide progress feedback for each major step:
  - checking repository
  - reading status
  - collecting branch/message
  - confirming operation
  - staging changes
  - creating commit
  - pushing to remote
- Use concise, readable status sections.
- Clearly mark final outcome as success or failure.
- On success, report:
  - commit branch
  - target remote branch
  - short commit summary if available
- On failure, report:
  - which step failed
  - the likely cause
  - recommended next action

## Security requirements

- Never display secrets from environment variables, Git remotes, credentials helpers, or config files.
- If a remote URL includes embedded credentials, redact them in any user-facing output.
- Never log `.git-credentials`, SSH private key contents, or token values.
- Do not modify credential helpers unless explicitly asked.
- Do not commit files that obviously contain secrets without warning the user.

## Suggested implementation pattern

1. Detect repository and branch.
2. Read `git status --short` and summarize file states.
3. Ask user for target branch and commit message.
4. Confirm the full operation.
5. Run staging.
6. Run commit.
7. Run push.
8. Interpret failures carefully and explain them clearly.

## Example interaction

User intent: Push my local changes to GitHub.

Assistant flow:

1. Check whether the folder is a Git repo.
2. Summarize current status.
3. Ask for:
   - commit message
   - target branch
4. Confirm the operation.
5. Execute commit and push.
6. Report success or provide targeted remediation steps.

## Output expectations

A strong execution should produce:

- a clear summary of pending changes
- a user-supplied commit message
- explicit branch selection
- visible progress updates
- safe handling of Git push errors
- no credential leakage
