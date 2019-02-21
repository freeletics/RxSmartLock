#!/bin/bash
#
# Deploy a jar, source jar, and javadoc jar to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq and
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/

SLUG="freeletics/RxSmartLock"
JDK="oraclejdk8"
BRANCH="master"

set -e

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping deployment: was pull request."
elif [ "$TRAVIS_BRANCH" != "$BRANCH" ]; then
  echo "Skipping  deployment: wrong branch. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
else
  echo "Deploying ..."
  openssl aes-256-cbc -K $encrypted_7e5175dca760_key -iv $encrypted_7e5175dca760_iv -in Freeletics.gpg.enc -out Freeletics.gpg -d
  gpg --import Freeletics.gpg
  echo "signing.password=$PGP_KEY" >> rxsmartlock/gradle.properties
  echo "signing.secretKeyRingFile=/home/travis/.gnupg/secring.gpg" >> rxsmartlock/gradle.properties
  echo "org.gradle.parallel=false" >> gradle.properties
  echo "org.gradle.configureondemand=false" >> gradle.properties

  echo "travis content"
  find /home/travis/.gnupg/
  
  ./gradlew --stop
  ./gradlew :rxsmartlock:uploadArchives --no-daemon --no-parallel -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false
  rm Freeletics.gpg
  git reset --hard
  echo "Deployed!"
fi