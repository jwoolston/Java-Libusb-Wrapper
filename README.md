# Android-Libusb-Wrapper
Java wrapper for libusb library following Android USB Host API

## Status
### Master
[![CircleCI](https://circleci.com/gh/jwoolston/Android-Libusb-Wrapper/tree/master.svg?style=svg)](https://circleci.com/gh/jwoolston/Android-Libusb-Wrapper/tree/master)
[![codecov](https://codecov.io/gh/jwoolston/Android-Libusb-Wrapper/branch/master/graph/badge.svg)](https://codecov.io/gh/jwoolston/Android-Libusb-Wrapper)
### Development
[![CircleCI](https://circleci.com/gh/jwoolston/Android-Libusb-Wrapper/tree/development.svg?style=svg)](https://circleci.com/gh/jwoolston/Android-Libusb-Wrapper/tree/development)
[![codecov](https://codecov.io/gh/jwoolston/Android-Libusb-Wrapper/branch/development/graph/badge.svg)](https://codecov.io/gh/jwoolston/Android-Libusb-Wrapper)

## Overview
This is wrapper of the [libusb](https://github.com/libusb/libusb) library for Android which provides a pure Java interface. The goal of this project is to avoid the shortcomings of the Android USB Host framework which notably are:
* No support for Isochronous transfers
* Control transfers are always synchronous
* Many useful API functions were added in several different Android versions
* Function return values are extremely vague. Many are either boolean, with false being for any of the dozen or more errors possible, or -1 for the same possible error set.

The Android USB Host framework is generally well formatted and convenient to use (In my opinion) and I have used it extensively. For this reason, I have chosen to emulate its API wherever possible and noticeable portions of code (and API documentation) have been copied from the [Android Open Source Project](https://android.googlesource.com/platform/) and I owe them thanks for the public visibility and permissive licensing, as well as inspiration for this API.

While based on the Android API, there are some differences that are necessitated, primarily due to how the Android USB permission model functions. These differences
are kept to a minimum and are clearly documented. Most notable is this libraries `UsbManager` vs. the one in the Android framework. Since this library has no support for
Android Accessory mode and the permission model prevents us from managing the USB port on the phone, all API related to that and permissions is excluded.

## Main Feature Set (Planned)
1. Dependency free Mavenized Android library with Java only interface. This means the libusb .so files will come with this artifact.
2. Root access **is not** required.
2. Support for most USB Host capable Android devices. Minimum API is 14.

## Checkout
This repository uses a submodule to another one containing libusb. When checking out this library, either have your Git software recurse the submodules or after checkout you can run
`git submodule update --init`

## Building
Due to the NDK requirement, building this project will require you to have the Android NDK on your machine. Setup of the NDK is left to the user. See [Google's NDK website](https://developer.android.com/tools/sdk/ndk/index.html) for more information on this.

After setting up the NDK, you will need to reference it in your version of `local.properties` by declaring `ndk.dir`.

Following this, the project should build successfully.

## Open Source Credits
- Configuration of builds and deployment was done by [ToxicBakery](https://github.com/ToxicBakery). Additionally, he has provided a general sounding board and motivation to enlarge the scope of this project to provide a hopefully more useful library to the community.

- The underlying libusb has been modified based on development work by another community member [vianney](https://github.com/vianney/libusb/tree/android) For convenience, I maintain my own for of libusb with his modifications and use it as the libusb source for this library. As libusb is updated, those changes will be pulled in as well.

- Testing of a number of features in the `library` module is done against the USB MSC class primarily because they are _relatively_ simple and provide a large data source/sink with reasonable speed capabilities. To save the effort of developing the boiler plate code needed to issue the SCSI commands to these devices, a module (`msc_test_core`) is included which contains a modified copy of of [libaums](https://github.com/magnusja/libaums) by GitHub user [magnusja](https://github.com/magnusja). This library was not used as is primarily because it utilizes the Android USB APIs internally and for testing we want to use the APIs provided by this library. The change in package name is exclusively because I do not wish to in any way conflict with his namespace, either intentionally or accidentally.
