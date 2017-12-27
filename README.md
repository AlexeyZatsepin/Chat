# Chat
[![CircleCI](https://circleci.com/gh/AlexeyZatsepin/Chat.svg?style=svg&circle-token=f1898a742abd8c2313f26a8b21cb2000080e1293)](https://circleci.com/gh/AlexeyZatsepin/Chat)
[![CircleCI](https://circleci.com/gh/AlexeyZatsepin/Chat.svg?style=shield&circle-token=f1898a742abd8c2313f26a8b21cb2000080e1293)](https://circleci.com/gh/AlexeyZatsepin/Chat)

This repository contains code from [Firebase web codelab](https://codelabs.developers.google.com/codelabs/firebase-android/#0), [Firebase Android codelab](https://codelabs.developers.google.com/codelabs/firebase-web/#0),  [Firebase in a Weekend: Android by Google](https://www.udacity.com/course/firebase-in-a-weekend-by-google-android--ud0352) Udacity course, and smth from me. :)
There are common chat application for both of Android and Web.

## Overview

Chat is an app that allows users to send and receive text and photos in realtime across platforms. In this project I test all features that provides by Firebase.
You can check [web client](https://friendlychat000.firebaseapp.com), [android client](https://12-111722923-gh.circle-artifacts.com/0/apks/app-release-unsigned.apk).

## Setup

It uses firebase services, so requires creating a Firebase project. See https://firebase.google.com/ for more information.

##### Build Android client:
$ gradlew assembleRelease

##### Deploy server:
$ firebase deploy

##### Test web client localy:
$ firebase serve

## License
See [LICENSE](LICENSE)

