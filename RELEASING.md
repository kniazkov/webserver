# Release Process

This checklist is for project maintainers.

## Prerequisites

- Java 21 and Maven 3.9 or later.
- A GPG key matching the project identity.
- A Central Portal user token stored under the `central` server ID in Maven
  `settings.xml`.
- A clean working tree on `master`.

The Central Portal credentials have this form:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>${env.CENTRAL_USERNAME}</username>
            <password>${env.CENTRAL_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

## Prepare

1. Choose a Semantic Versioning release number.
2. Set the same version in `pom.xml`, its SCM tag, `README.md`, and examples.
3. Move the release notes from `Unreleased` into a dated changelog section.
4. Update changelog comparison links.
5. Run the complete gate:

```bash
mvn clean verify -Pe2e
```

6. Merge the release-preparation pull request and confirm that CI passes on
   `master`.

## Publish

From the exact commit that passed CI, build, sign, and upload the deployment to
the Central Portal:

```bash
mvn clean deploy -Pe2e,release
```

The `release` profile attaches source and Javadoc artifacts, signs all release
files, and uploads a bundle using the `central` credentials. Automatic
publishing is disabled. Inspect the validated deployment in the Central Portal
and publish it manually.

Maven Central releases are immutable. Do not reuse a version after any artifact
with that version has been published.

## Tag and Announce

After the Central deployment is published:

```bash
git tag -a v2.0.0 -m "Foundry19 Web Server 2.0.0"
git push origin v2.0.0
```

Create a GitHub release from the tag using the matching changelog section.
Finally, verify that `com.kniazkov:webserver:2.0.0` resolves from Maven Central
in a clean project.
