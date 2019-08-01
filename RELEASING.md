# Releasing

1. Change the version in gradle.properties to a non-SNAPSHOT version.
2. Update the README.md with the new version.
3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
4. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
6. Update the gradle.properties to the next SNAPSHOT version.
7. `git commit -am "Prepare next development version."`
8 `git push && git push --tags`
9. Update the sample app to the release version and send a PR.