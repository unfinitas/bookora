# Contribution Guide for Bookora

To keep the project clean and maintainable, please follow the guidelines below.

---

## 1. Workflow

1. **Fork the repository** if you don’t have direct write access.
2. **Create a new branch** for your task using the JIRA format:

```bash
git checkout -b <PROJECT-ID>-<ID>--<description>
```

**Branch naming rules:**

* `<PROJECT-ID>`: 2+ uppercase letters (e.g., PROJ, ABC)
* `<ID>`: numeric ticket/issue number (e.g., 123)
* `<description>`: lowercase words separated by single dashes (e.g., add-login-api)
* Use double dash `--` between ID and description
* Max length: 80 characters

3. **Work on a single task per branch**. Each task should correspond to **one commit only**.

---

## 2. Commit Guidelines

We follow **Conventional Commits** (semantic commit messages):

```
<type>(<scope>): <short description>
```

### Commit Types

| Type     | When to use                                             |
| -------- | ------------------------------------------------------- |
| feat     | A new feature                                           |
| fix      | A bug fix                                               |
| docs     | Documentation only                                      |
| style    | Formatting, missing semicolons, lint fixes              |
| refactor | Code change that neither fixes a bug nor adds a feature |
| test     | Adding or updating tests                                |
| chore    | Maintenance tasks (build, CI, etc.)                     |

### Examples

```bash
git commit -m "feat(auth): add signup endpoint"
git commit -m "fix(user): handle null email error"
git commit -m "docs(readme): update contribution guide"
git commit -m "style(button): fix indentation in login page"
```

**Rules**:

* One task → one commit
* Commit message **must be clear and concise**
* Avoid multiple unrelated changes in one commit

---

## 3. Pull Requests

1. Push your branch:

```bash
git push origin <PROJECT-ID>-<ID>--<description>
```

2. Open a Pull Request (PR) against the `main` branch with the JIRA-style name:

```
<PROJECT-ID>-<ID>: Description
```

3. Include a description explaining the task and what has been changed.
4. PRs should be **reviewed and approved** before merging.
5. Squash commits **only if there are minor corrections**; ideally, each task should already be a single clean commit.

---

## 4. Code Style & Quality

* Follow project’s **linting rules**
* Run **tests** before submitting a PR
* Keep code **modular and readable**
* **All commits must be signed**

---

## 5. Summary

* **One task = one commit**
* Use **semantic commit messages**
* Branch names: `<PROJECT-ID>-<ID>--<description>` (uppercase project ID, numeric ID, lowercase dash-separated description, max 80 chars)
* PR names: `<PROJECT-ID>-<ID>: Description`
* Keep branches focused
* Always create a PR with a clear description
