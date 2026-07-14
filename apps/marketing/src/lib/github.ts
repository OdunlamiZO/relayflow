const GITHUB_API_BASE_URL = "https://api.github.com";

/**
 * Whether a GitHub username exists — unauthenticated public API, no token
 * needed. Fails open (returns true) on anything other than a confirmed 404,
 * so a GitHub outage or rate limit doesn't block checkout; a real typo
 * still gets caught before payment either way, since a wrong username also
 * fails the manual access grant later.
 */
export async function githubUserExists(username: string): Promise<boolean> {
  try {
    const response = await fetch(
      `${GITHUB_API_BASE_URL}/users/${encodeURIComponent(username)}`,
      { headers: { Accept: "application/vnd.github+json" } }
    );

    return response.status !== 404;
  } catch (error) {
    console.error(`Failed to verify GitHub username ${username}:`, error);

    return true;
  }
}
