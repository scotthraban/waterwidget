Water Widget
============

A widget to help quickly enter water consumption log entries, synchs with FITBIT® water consumption logs. Fitbit is a registered trademark and service mark of Fitbit, Inc. Water Widget is designed for use with the Fitbit platform. This product is not put out by Fitbit, and Fitbit does not service or warrant the functionality of this product.

Usage
=====

I don't mind if you use this code, subject to the license terms of course, but you will need to change the package structure so as to not clash with my widget out in the wild, should I ever decide to actually put it out on Google Play.

You will need to register your own Application with dev.fitbit.com, and then put your oauth key and secret in a assets/fitbit.client.properties file like so:

    oauth.consumer.key=xxxxxx
    oauth.consumer.secret=xxxxxx

Tips for building yourself
==========================

The project at this point must be built from inside the Android Developers Tools, and pom.xml is really only there to assist in copying the dependencies into the libs directory, so that does not need to be done manually.

The PNG files used in the project were created using GIMP, and the GIMP XCF source files are located under the images directory.

Be aware that the Fitbit4j library that I am using is from a fork of the official Fitbit4j library, as it did not have all the features that were needed (specifically the ability to get/update the water consumption log entries), and there was a defect (specifically the fetch of the water units was not correct). All changes that I have made to the library on my fork have been submitted back to the official Fitbit4J project via pull requests:

https://github.com/Fitbit/fitbit4j/pull/9

https://github.com/Fitbit/fitbit4j/pull/10

The original Fitbit4J library can be downloaded from Fitbit, and is also available on GitHub at https://github.com/Fitbit/fitbit4J.

My fork is available at https://github.com/scotthraban/fitbit4j.
