# Change Log

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