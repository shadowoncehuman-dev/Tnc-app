---
name: Vite workspace hooks defensive pattern
description: How to handle TanStack Query hooks returning unexpected values in vite dev mode with pnpm workspace packages
---

## The Problem

In pnpm monorepos, `@workspace/*` packages exported as TypeScript source (not compiled) can cause "Invalid hook call" warnings in React when `@tanstack/react-query` hooks are called from those packages in vite dev mode. The root cause is subtle: vite's pre-bundler may create multiple React module instances across the workspace boundary even with `resolve.dedupe` and `preserveSymlinks: false`.

**Why:** When the workspace package is a symlink outside vite's `root` directory, esbuild's dep optimization phase may resolve `react` and `@tanstack/react-query` via a different path than the main app, creating separate `__commonJS` factories even though they point to the same physical file.

## Symptom

```
Invalid hook call. Hooks can only be called inside of the body of a function component.
TypeError: (data ?? []).filter is not a function
```

The second error happens because `useQuery` returns an unexpected non-null, non-undefined value (not an array) when the hook dispatch is corrupted.

## The Fix

**Defensive Array.isArray() checks** in every page that consumes list data from hooks:

```ts
// WRONG — fails when hook returns unexpected non-null value
const filtered = (courses ?? []).filter(...)

// CORRECT — handles any return value gracefully
const courseList = Array.isArray(courses) ? courses : [];
const filtered = courseList.filter(...)
```

Apply this to EVERY array operation on hook data. Use `Array.isArray(x) ? x : []` instead of `x ?? []`.

## Other mitigations tried

- `resolve.dedupe: ["react", "react-dom", "@tanstack/react-query", "@tanstack/query-core"]` — not sufficient alone
- `resolve.preserveSymlinks: false` — not sufficient alone
- Moving `@tanstack/react-query` to `peerDependencies` in the workspace package — not sufficient alone
- Copying workspace package files into the app's src — not sufficient alone
- All of the above together improve stability but the defensive Array.isArray() check is required

## How to apply

Whenever a page calls `useGetCourses()`, `useListSessions()`, `useGetUserPurchases()`, or any hook that returns a list, wrap array operations with `Array.isArray()` before calling `.filter`, `.map`, `.length`, etc.
