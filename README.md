# ZeroKit SDK for Android
**[ZeroKit](https://tresorit.com/zerokit/)** is a simple, breach-proof user authentication and end-to-end encryption library.

The **ZeroKit SDK** for Android is currently under development and is accessible as a preview. We continuously improve the SDK and fix bugs.

You can [sign up for **ZeroKit** here.](https://tresorit.com/zerokit/)

## Content
- [Requirements](#requirements)
- [Download](#download)
- [Usage](#usage)
- [Identity Provider](#identity-provider)
- [Administrative API](#administrative-api)
- [Example Application](#example-application)
- [Changelog](#changelog)
- [Contact](#contact)
- [License](#license)

## Requirements
**Android SDK**: The Zerokit SDK library is compatible from API 21 (Android 5.0 - Lollipop).

## Download
Add the dependency to `build.gradle`
```groovy
dependencies {
    compile 'com.tresorit.zerokit:zerokit:4.1.2'
}
```

## Usage
### Initializing ZeroKit
To initialize the SDK you will need your **API URL**:
In `AndroidManifest.xml`, add the following element as a child of the `<application>` element, by inserting it just before the closing `</application>` tag:
*AndroidManifest.xml*
```xml
<meta-data
     android:name="com.tresorit.zerokitsdk.API_ROOT"
     android:value="YOUR API ROOT HERE (eg. https://{tenantid}.api.tresorit.io)"/>
```

#### Permissions
The following permissions are defined in the Zerokit SDK manifest, and are automatically merged into your app's manifest at build time. You **don't** need to add them explicitly to your manifest:

* [android.permission.INTERNET](https://developer.android.com/reference/android/Manifest.permission.html#INTERNET) - Used by the API to communicate with the service.
* [android.permission.ACCESS_NETWORK_STATE](https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_NETWORK_STATE) - Allows the API to check the connection status in order to determine connection errors

#### Proguard
If you are using ProGuard you might need to add the following option:
```
-dontwarn com.tresorit.zerokit.**
```
### Using ZeroKit SDK
The API provides 2 possible ways to use the Zerokit SDK:

*Asynchron*
```java
Zerokit.getInstance().encrypt(tresorId, "apple").execute(
    cipherText -> Log.d("Zerokit", String.format("Encrypted text: %s", cipherText)),
    error -> Log.d("Zerokit", String.format("Encrypting failed: %s", error.getMessage())));
```
*Synchron*
```java
Response<String, ResponseZerokitError> response = Zerokit.getInstance().encrypt(tresorId, "apple").execute();
if (response.isError()) Log.d("Zerokit", String.format("Encrypting failed: %s", response.getError().getMessage()));
else Log.d("Zerokit", String.format("Encrypted text: %s", response.getResult()));
```

#### Password handling
A core concept of ZeroKit is that your application should not access and pass around the users' passwords. All password handling should be done by ZeroKit. For this we provide a `PasswordEditText` UI component that you should present to users to enter their passwords. If you are implementing data-binding solutions in your application and you do not want to use view ids, you can utilize `PasswordHandler` to achieve the same result. 

With this solution you can ensure that the password will not be stored in `String` in Java side.

Some tips and examples for using `PasswordEditText` and/or `PasswordHandler`:

_View_
```xml
<com.tresorit.zerokit.PasswordEditText
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    ...
```
```java
Zerokit.getInstance().login(userId, passwordEditText).subscribe(responseLogin -> {...});
```
_Data Binding_
```xml
<com.tresorit.zerokit.PasswordEditText
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:passwordExporter="@{viewmodel.passwordExporter}"
    ...
```
```java
Zerokit.getInstance().login(userId, passwordExporter).subscribe(responseLogin -> {...});
```
It is also possible to securely compare two `PasswordEditText` and/or `PasswordHandler` without access of the concrete content in it.
It can be useful when for example password confirmation is required:
```java
passwordExporter.isContentEqual(passwordExporterConfirm)
```
```java
passwordEditText.isContentEqual(passwordEditTextConfirm)
```

## Identity Provider
ZeroKit comes with OpenID Connect provider implementation that you can use in your app. Use the `getIdentityTokens(String clientId)` method of a `ZeroKit` object to get authorization code and identity token for the current ZeroKit user. A user must be logged in when calling this method. The ZeroKit Open ID client used in mobile apps should have the following settings:

- Redirect URL should have the following format: `https://{Client ID}.{Tenant ID}.api.tresorit.io/`
- Flow should be set to `Hybrid`
- You can optionally turn on `Requires proof key (DHCE)`

You can add new clients and edit settings on the management portal.

## Administrative API
Most of the cryptographic operations (including invites and sharing) must be done client side by the SDK library. To provide control over these operations, and to prevent possible abuse by tampering the client, we introduced the admin API. All client initiated changes which has a permanent effect on the server has to be approved through the Admin API (typically by the server backend of the integrated app). For more information see the ZeroKit [documentation](https://tresorit.com/files/zerokit_encryption-sdk-documentation.pdf).

## Example Application
An example application is included with ZeroKit to demonstrate its usage. It demonstrates the following features:
- Registration
- Login and logout
- Tresor creation
- Tresor sharing
- Encryption
- Decryption

### Configuring the Example
In the `app/src/main/zerokit.properties` set the values of `apiroot`, `clientid`, `appbackend` and `objectserver`. If this file does not exist, let’s create one with the same name.
```
apiroot=your base url (e.g. https://{tenantid}.api.tresorit.io)
clientid=client id for your openid
appbackend= url of the sample application backend (e.g. http://10.0.2.2:3000)
```

- `apiroot`: This is your _ZeroKit Service URL_. You can find this URL on the management portal.
- `clientid`: This is the _client ID_ for your _OpenID Connect client_ that you wish to use with your mobile app. You can find this value on the basic configuration page of your _tenant_ at [here](https://manage.tresorit.io)
- `appbackend`: This is the address of the sample _application backend_.

Now you are ready to **Build and Run** the example in Android Studio.

#### Registering Test Users
Register test users following the `'test-user-{XYZ}'` username format. These users will be automatically validated by the sample backend so you can log in right after registration.

##### Used 3rd party libraries in the Example Application
- [Dagger 2](https://github.com/google/dagger): Dependency injector for Android
- [EventBus](https://github.com/greenrobot/EventBus): A publish/subscribe event bus
- [Retrofit](https://github.com/square/retrofit): Type-safe HTTP client for Android and Java

## Changelog
See the [CHANGELOG.md](./CHANGELOG.md) file.

## Contact us
Do you have any questions? [Contact us](mailto:zerokit@tresorit.com) (zerokit@tresorit.com)

## License
See the [LICENSE.txt](./LICENSE.txt) file.