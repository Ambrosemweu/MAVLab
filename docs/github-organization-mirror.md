# GitHub repository mirror

The `Mirror to Personal Repository` workflow keeps:

- Primary source: `Labs-Ascend/MAVLab`
- Personal mirror: `Ambrosemweu/MAVLab`

in sync after branch pushes, tag pushes, and branch or tag deletions.

## One-time credential setup

1. Create a fine-grained personal access token owned by `Ambrosemweu` with
   access to `Ambrosemweu/MAVLab`.
2. Grant the token repository `Contents: Read and write` permission. Also grant
   workflow-file write permission if GitHub presents it separately.
3. In `Labs-Ascend/MAVLab`, open **Settings > Secrets and variables >
   Actions**.
4. Add a repository secret named `MAVLAB_MIRROR_TOKEN`.
5. Open **Actions > Mirror to Personal Repository** and run the workflow once.

The token belongs only in the Actions secret. Do not add it to Git remotes,
workflow files, or local configuration.

## Repository rules

If `Ambrosemweu/MAVLab` protects branches or tags, allow the token owner to
bypass the personal mirror's rules. The mirror intentionally makes the personal
repository match the organization source, including deleting branches or tags
that were removed from the source.

Developers should clone and push to `Labs-Ascend/MAVLab`. Do not push directly
to `Ambrosemweu/MAVLab`, because the next mirror run can overwrite those refs.
