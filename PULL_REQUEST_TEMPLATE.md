
# Getting your pull-requests reviewed and merged

<details>
<summary>Definition in Airsonic</summary>

<blockquote>

Once you have submitted a pull-request, a number of factors may determine how
much attention it receives, how quickly it may be reviewed, and whether or not
it eventually gets accepted and merged. Here are a few guidelines that can help
speed the process and increase the chances of a pull request being accepted and
merged in a timely fashion:

- Limit the scope to the minimum changes necessary to accomplish a discrete and
	well defined task.
- If your changes could be broken down into smaller units, then they are much
	less likely to be accepted.
- Try not to address unrelated issues in the same set of changes, unless
	failing to do so would cause other problems (like merge conflicts).
- Do your best to maintain prior functionality while eliminating (or at least
	minimizing) side effects.
- Changes that affect backward compatibility or make it more difficult to
	upgrade or downgrade versions of libraries or installations will be the most
	heavily scrutinized and take the longest.
- Maintain the style and coding standards represented by the codebase.
- Consistent, simple, and easy-to-understand changes are usually preferred.
- Do not mix functional changes with code cleanups or style changes.
- Make it as easy as possible for others to review your changes. Rebasing your
	PR to address issues is strongly preferred over adding additional commits
	that make changes to (or undo parts of) prior commits.
- In general, the more commits in a PR, the harder it is to review and the
	longer it will take to be reviewed. But do not sacrifice good change
	isolation by combining commits unnecessarily.
- If your PR needs more than 2 or 3 commits, then you *probably* need to reduce
	the scope of your PR. If a single commit touches more than a few files or
	more than 30-50 lines, then the scope of the commit *probably* needs to be
	reduced.
- Keep in mind that we strive to balance stability with new features while best
	utilizing everybody's limited available free time. As such, a pull request may
	be rejected if it doesn't strike that balance.

And finally:

- Actively maintain your PR. If any concerns are raised, or tests fail, or some
	other change occurs in the codebase that affects your PR (like a merge
	conflict) before your PR is accepted and merged, you are expected to address
	and resolve those issues or the PR may be rejected.

Once all concerns have been addressed, having a change accepted usually
requires two (or more, depending on complexity and impact) [core
contributors](https://github.com/airsonic/airsonic/graphs/contributors): one to
explicitly approve the pull-request, and another to perform the actual merge.

If you keep sending great code, you might be invited to become a *core
contributor*.

Normal releases do not happen on any fixed schedule. They happen when the
maintainers collectively decide that enough changes and testing have taken
place that a new release is warranted. Bugfix releases (when a problem has been
discovered that is likely to impact users significantly), may happen more
quickly if needed. Even after acceptance, the inclusion of larger changes may
be delayed until a major version release in order to ensure that the impact to
users is minimized and that stability is maintained.

</blockquote>

</details>

By the way, Jpsonic does not accept pull requests at this time.
This is entirely a matter of my capacity. Accepting indiscriminate requests is because we have determined that the side effects outweigh the slight benefits.
If you have a request that involves a big architect change rather than a small feature, and it's a very groundbreaking idea that benefits a lot of people, and most of the design/implementation/verification work is done by you alone, then Please contact us.
