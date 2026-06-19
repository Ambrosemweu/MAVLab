# GitHub organization mirror

The `Mirror to Labs Ascend` workflow keeps:

- Source: `Ambrosemweu/Drone-SIM`
- Mirror: `Labs-Ascend/MAVLab`

in sync after branch pushes, tag pushes, and branch or tag deletions.

## One-time credential setup

1. Create a fine-grained personal access token with access to
   `Labs-Ascend/MAVLab`.
2. Grant the token repository `Contents: Read and write` permission. Also grant
   workflow-file write permission if GitHub presents it separately.
3. If the organization uses SAML SSO, authorize the token for `Labs-Ascend`.
4. In `Ambrosemweu/Drone-SIM`, open **Settings > Secrets and variables >
   Actions**.
5. Add a repository secret named `MAVLAB_MIRROR_TOKEN`.
6. Open **Actions > Mirror to Labs Ascend** and run the workflow once.

The token belongs only in the Actions secret. Do not add it to Git remotes,
workflow files, or local configuration.

## Repository rules

If `Labs-Ascend/MAVLab` protects branches or tags, allow the token owner to
bypass the mirror repository's rules. The mirror intentionally makes the
organization repository match the source, including deleting branches or tags
that were removed from the source.
