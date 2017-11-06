# Change Log
## [4.1.3] - 2017-11-06
### Added
- **Encrypt and Decrypt byte methods**: New methods for encrypt and decrypt byte arrays instead string
- Using the new **v5 api**

## [4.1.2] - 2017-07-14
### Added
- **Invitation links**: Invitation links are used to invite someone into a tresor who is not a registered user. This method of invitation is made this way to communicate a best-practice. Using the link format below (placing the secret inside the fragment identifier of the url) you can ensure that the credentials necessary to get access to the tresor doesn't travel to your server and subsequently through the network. We advise, that you don't store any of the invitation links on your server unencrypted as it is a security critical information, which, in case of a breach, can be used to get access to user data. Even so, to achieve the best security you should use only password protected links and ask the users to transfer the password and the link to the invitee through different channels (e.g.: email the link and text/phone the password).
- **Easier initialization**: In the sample app you can use only the  _`/src/main/zerokit.properties`_ file

## [4.1.1] - 2017-05-04

### Fix
- JSON escaping issue

## [4.1.0] - 2017-04-06

### Added
- **Identity provider**: ZeroKit comes with OpenID Connect provider implementation that you can use in your app. Use the `getIdentityTokens(String clientId)` method of a `ZeroKit` object to get authorization code and identity token for the current ZeroKit user.
- **Sample backend**: The example app requires a backend to function. We created a sample backend that you can use for the mobile and web ZeroKit sample apps. You can find the backend and setup instructions [here](https://github.com/tresorit/ZeroKit-NodeJs-backend-sample).

### Removed
- Old Administrative API (*use the Sample backend instead of this*)
## [4.0.3] - 2017-03-24
### Changed
- Observable API removed
- Instead of this you can use sync and async calls with the new API:
    - old async:
    ```java
    Zerokit.getInstance().encrypt(tresorId, "apple").subscribe(
        cipherText -> Log.d("Zerokit", String.format("Encrypted text: %s", cipherText)),
        error -> Log.d("Zerokit", String.format("Encrypting failed: %s", error.getMessage())));
    ```
    - new async:
    ```java
    Zerokit.getInstance().encrypt(tresorId, "apple").execute(
        cipherText -> Log.d("Zerokit", String.format("Encrypted text: %s", cipherText)),
        error -> Log.d("Zerokit", String.format("Encrypting failed: %s", error.getMessage())));
    ```
    
    - old sync:
    ```java
    Result<String, ResponseZerokitError> result = Zerokit.getInstance().sync().encrypt(tresorId, "apple");
    if (result.isError()) Log.d("Zerokit", String.format("Encrypting failed: %s", result.getError().getMessage()));
    else Log.d("Zerokit", String.format("Encrypted text: %s", result.getResult()));
    ```
    - new sync:
    ```java
    Response<String, ResponseZerokitError> response = Zerokit.getInstance().encrypt(tresorId, "apple").execute();
    if (response.isError()) Log.d("Zerokit", String.format("Encrypting failed: %s", response.getError().getMessage()));
    else Log.d("Zerokit", String.format("Encrypted text: %s", response.getResult()));
    ```