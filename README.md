# ZeroKit SDK for Android
**[ZeroKit](https://tresorit.com/zerokit/)** makes it easy to implement end-to-end encryption in your application.

The **ZeroKit SDK** for Android is currently under development and is accessible as a preview. We continuously improve the SDK and fix bugs.

You can [sign up for **ZeroKit** here.](https://tresorit.com/zerokit/)
## Requirements
**Android SDK**: The Zerokit SDK library is compatible from API 21 (Android 5.0 - Lollipop).
## Changelog
See the [CHANGELOG.md](./CHANGELOG.md) file.
## Download
Add the dependency to `build.gradle`
```groovy
dependencies {
    compile 'com.tresorit.zerokit:zerokit:4.0.3'
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
## Example Application
An example application is included with ZeroKit to demonstrate its usage. It demonstrates the following features:
- Registration
- Login and logout
- Tresor creation
- Tresor sharing
- Encryption
- Decryption

### Configuring the Example
To use the example app, first you have to set it up with your ZeroKit configuration.
In the `sample/src/main/AndroidManifest.xml` set the values for `com.tresorit.zerokitsdk.API_ROOT`.
```xml
<meta-data
     android:name="com.tresorit.zerokitsdk.API_ROOT"
     android:value="YOUR API ROOT HERE (eg. https://{tenantid}.api.tresorit.io)"/>
```
In the `sample/src/main/assets/zerokit.properties` set the values for `adminkey` and `adminuserid`. If this file does not exist, copy the sample `zerokit.demo.properties` file in the same directory to create one:
```
adminkey=YOUR ADMIN KEY HERE (eg. 32467af34fadec34526cf789c78ef89...)
adminuserid=YOUR ADMIN USER ID HERE (eg. admin@{tenantid}.tresorit.io)
```
**!!! IMPORTANT NOTE:** You must **never include your Admin key in your application**. All Admin key calls must be done by your backend. We implemented a mock application in this example so you can run it without setting up a server. The admin key must be kept secret and not included in any client applications that you distribute.

Now you are ready to **Build and Run** the example in Android Studio.
##### Used 3rd party libraries in the Example Application
- [Dagger 2](https://github.com/google/dagger): Dependency injector for Android
- [EventBus](https://github.com/greenrobot/EventBus): A publish/subscribe event bus

## Contact us
Do you have any questions? [Contact us](mailto:zerokit@tresorit.com) (zerokit@tresorit.com)

## License
See the [LICENSE.txt](./LICENSE.txt) file.