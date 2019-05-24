# SAP Cloud Platform SDK for Android Offline OData Sample

## Description

This sample app showcases the functionality of an [Offline OData](https://help.sap.com/viewer/70ac991a4f734773b1892a8d0d45eabc/Cloud/en-US/4b2ef65d23eb48b5adbdd6e83fa5ff20.html) app.  The first time the app opens, it downloads a portion of the backend's data, enabling the user to make changes to the data locally. The user doesn't need a working internet connection to make changes once the initial download has completed.  Working against an on device offline store instead of a remote backend can improve performance.  When an internet connection is available, the user can synchronize their local changes with the backend.

This sample app will show how to indicate to the user which data has changed locally and has yet to be synchronized with the backend.  Additionally, it will show how to deal with differences between data in the user's local database and the backend.

If you are new to the SAP Cloud Platform SDK for Android, [Get Started with SAP Cloud Platform SDK for Android](https://developers.sap.com/mission.sdk-android-get-started.html), [Step by Step with the SAP Cloud Platform SDK for Android](https://blogs.sap.com/2018/10/15/step-by-step-with-the-sap-cloud-platform-sdk-for-android-part-1/) series, and the [SAP Cloud Platform SDK for Android Learning Journey](https://help.sap.com/doc/221f8f84afef43d29ad37ef2af0c4adf/HP_2.0/en-US/747d6d2ea0534ba99612920c7402631a.html) are great places to start.

## The Finished Product

The app enables customer info to be viewed and edited.

![App home screen](images/customer_screen.png)

Let's start setting up this project!

## Requirements

* [Android Studio](https://developer.android.com/studio/index.html) version 3.4
* [SAP Cloud Platform SDK for Android from Trial Downloads](https://www.sap.com/developer/trials-downloads/additional-downloads/sap-cloud-platform-sdk-for-android-15508.html) or [SAP Cloud Platform SDK for Android from Software Downloads](https://launchpad.support.sap.com/#/softwarecenter/template/products/_APP=00200682500000001943&_EVENT=NEXT&HEADER=Y&FUNCTIONBAR=Y&EVENT=TREE&NE=NAVIGATE&ENR=73555000100800001281&V=MAINT&TA=ACTUAL/SAP%20CP%20SDK%20FOR%20AND) version 2.1.1
* [SAP Cloud Platform Trial](https://cloudplatform.sap.com/index.html)

The blog [Step by Step with the SAP Cloud Platform SDK for Android](https://blogs.sap.com/2018/10/15/step-by-step-with-the-sap-cloud-platform-sdk-for-android-part-1/) contains additional details on how to setup and install the SDK, how to register for a trial version of the SAP Cloud Platform, and how to enable Mobile Services.

## Setting Up Mobile Services

The initial mobile services configuration for the offline project is included in the project folder. In the mobile services cockpit, navigate to **Mobile Applications > Native/Hybrid** and click the **Import** button.

![Mobile Applications > Native/Hybrid > Import button](images/importing_project_config_mobile_services.png)

In the resulting prompt window, browse for the `mobile_services/com.sap.offline_1.0.zip`. Click **Save**.

![Browsing for mobile services configuration](images/browse_for_imported_ms_config.png)

The imported project configuration will have an OAuth security endpoint that does not match your user name, so next, change the OAuth endpoint in the security section of the application to your own account. To do so, remove the current OAuth configuration in the **Security** section and create another one. Leave everything blank and click **OK**.

![Deleting old oauth configuration](images/deleting_old_oauth_config.png)

![Adding oauth client](images/add_oath_client.png)

Click **Save**, and the rest of the details for the OAuth configuration such as **Authorization Endpoint**, **Token Endpoint**, **End User UI**, and **Redirect URL** will be filled in.

![Save the OAuth configuration](images/save_oauth_config.png)

## Configuration

Open the project in Android Studio.

To successfully run the application, the OAuth string constants in the application need to be changed to reflect the new values. In the project, press `Ctrl + N` on Windows, or `Command + O` on Mac, and navigate to `MainActivity.java` and change the constants at the top of the file to match your username and client ID.

![Update the constants at the top of the MainActivity file](images/update_oauth_constants.png)

Your username and `OAUTH_CLIENT_ID` string can be found in the mobile services cockpit, as shown below.

![The OAUTH_CLIENT_ID string located in the mobile services cockpit](images/oauth_client_id.png)

Run the project to deploy it onto an emulator or device.  For further details on the app see [DOCUMENTATION.md](DOCUMENTATION.md).

## Known Issues

No known major issues.

## How to obtain support

If you have questions/comments/suggestions regarding this app please
post them using the tag [SAP Cloud Platform SDK for Android](https://answers.sap.com/tags/73555000100800001281) or if you encounter an issue, you can [create a ticket](https://github.com/SAP/cloud-sdk-android-offline-odata/issues/new).

## License

Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the SAP Sample Code License except as noted otherwise in the [LICENSE file](LICENSE).
