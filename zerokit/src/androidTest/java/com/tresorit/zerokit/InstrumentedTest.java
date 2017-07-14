package com.tresorit.zerokit;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.call.Response;
import com.tresorit.zerokit.response.IdentityTokens;
import com.tresorit.zerokit.response.ResponseAdminApiError;
import com.tresorit.zerokit.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.response.ResponseAdminApiLoginByCode;
import com.tresorit.zerokit.response.ResponseZerokitChangePassword;
import com.tresorit.zerokit.response.ResponseZerokitCreateInvitationLink;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitInvitationLinkInfo;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokit.response.ResponseZerokitPasswordStrength;
import com.tresorit.zerokit.response.ResponseZerokitRegister;
import com.tresorit.zerokit.util.Holder;
import com.tresorit.zerokit.util.JSONObject;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTest {

    static private Zerokit zerokit;
    static private AdminApi adminApi;

    static private String pass01;
    static private String user01;
    static private String pass02;
    static private String user02;

    private Context targetContext;
    private BackgroundThreadExecutor backgroundThreadExecutor;

    private final static class BackgroundThreadExecutor implements Executor {
        private final Handler handler;

        BackgroundThreadExecutor() {
            HandlerThread handlerThread = new HandlerThread("Background thread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }

        @Override
        public void execute(final Runnable r) {
            final CountDownLatch signal = new CountDownLatch(1);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    r.run();
                    signal.countDown();
                }
            });
            try {
                signal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void init() throws Throwable {
        backgroundThreadExecutor = new BackgroundThreadExecutor();
        targetContext = InstrumentationRegistry.getTargetContext();

        if (adminApi == null) {
            Properties properties = new Properties();
            properties.load(targetContext.getAssets().open("zerokit.properties"));
            adminApi = AdminApi.init(properties.getProperty("appbackend", ""), properties.getProperty("clientid", ""));
        }

        if (zerokit == null) {
            Properties properties = new Properties();
            properties.load(targetContext.getAssets().open("zerokit.properties"));
            Zerokit.init(targetContext, properties.getProperty("apiroot", ""));
            zerokit = Zerokit.getInstance();
            zerokit.getIdlingResource();
        }

        assertNotNull(adminApi);
        assertNotNull(zerokit);

        initUsers();

        loginIfNeededTest(user01, pass01);
    }

    private void initUsers() {
        if (TextUtils.isEmpty(user01) && TextUtils.isEmpty(user02)){
            pass01 = USER_01_PASS;
            user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);
            pass02 = USER_02_PASS;
            user02 = registrationTest(getUserName(USER_02_ALIAS), pass02);
        }

        Assert.assertFalse(TextUtils.isEmpty(user01));
        Assert.assertFalse(TextUtils.isEmpty(user02));
        Assert.assertFalse(TextUtils.isEmpty(pass01));
        Assert.assertFalse(TextUtils.isEmpty(pass02));
    }

    private static final String USER_01_ALIAS = "test-user01";
    private static final String USER_02_ALIAS = "test-user02";
    private static final String USER_01_PASS = "password01";
    private static final String USER_02_PASS = "password02";

    private String getUserName(String userName) {
        return String.format("%s-%s", userName, UUID.randomUUID());
    }

    @Test
    public void testPasswordStrength () {
        getPasswordStrengthTest(pass01);
    }

    @Test
    public void testZerokitError () {
        logoutTest();
        Response<ResponseZerokitLogin, ResponseZerokitError> response = login_noassert(user01, pass01 + ".");
        Assert.assertTrue(response.isError());
        Assert.assertEquals(response.getError().toString(), new ResponseZerokitError().parse(response.getError().toJSON()).toString());
    }

    @Test
    public void testPasswordEditText () {
        PasswordEditText passwordEditText01 = getPasswordEditText(pass01);
        PasswordEditText passwordEditText02 = getPasswordEditText(pass02 + ".");
        passwordEditTextEqualTest(passwordEditText01, passwordEditText02, false);
        passwordEditTextEqualTest(passwordEditText02, passwordEditText01, false);

        Assert.assertEquals(false, pass01.length() == passwordEditText02.getPasswordExporter().length());
        Assert.assertEquals(false, pass01.length() == passwordEditText02.length());

        passwordEditText02.initText(pass01);
        passwordEditTextEqualTest(passwordEditText01, passwordEditText02, true);
        passwordEditTextEqualTest(passwordEditText02, passwordEditText01, true);

        Assert.assertEquals(true, pass01.length() == passwordEditText02.getPasswordExporter().length());
        Assert.assertEquals(true, pass01.length() == passwordEditText02.length());

        passwordEditText01.clear();
        Assert.assertTrue(passwordEditText01.isEmpty());
        Assert.assertTrue(passwordEditText01.getPasswordExporter().isEmpty());

        passwordEditText02.getPasswordExporter().clear();
        Assert.assertTrue(passwordEditText02.isEmpty());
        Assert.assertTrue(passwordEditText02.getPasswordExporter().isEmpty());

        passwordEditTextEqualTest(passwordEditText01, passwordEditText02, true);
        passwordEditTextEqualTest(passwordEditText02, passwordEditText01, true);
    }

    private void passwordEditTextEqualTest(PasswordEditText passwordEditText01, PasswordEditText passwordEditText02, boolean equals) {
        Assert.assertEquals(equals, passwordEditText02.isContentEqual(passwordEditText01));
        Assert.assertEquals(equals,passwordEditText02.isContentEqual(passwordEditText01.getPasswordExporter()));
        Assert.assertEquals(equals,passwordEditText02.getPasswordExporter().isContentEqual(passwordEditText01));
        Assert.assertEquals(equals,passwordEditText02.getPasswordExporter().isContentEqual(passwordEditText01.getPasswordExporter()));
    }

    @Test
    public void testGetPublicProfile () {
        getPublicProfile(user01);
    }

    @Test
    public void testStorePublicProfile () {
        storePublicProfileTest(user01);
    }

    @Test
    public void testStoreProfile () {
        storeProfileTest();
    }

    @Test
    public void testStoreData () {
        storeDataTest(user01);
    }

    @Test
    public void testChangePasswordLoggedIn() {
        changePasswordTestWithLoggedIn(user01, pass01);
    }

    @Test
    public void testChangePasswordLoggedOut()  {
        changePasswordTestWithLoggedOut(user01, pass01);
    }

    @Test
    public void testCreateTresorAndEncryptText() {
        encryptTest(createTresor(), "textToEncrypt01");
    }

    @Test
    public void testShareTresorAndDecryptText() {
        String tresor01 = createTresor();
        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);
        shareTresor(tresor01, user02);
        logoutTest();
        loginTest(user02, pass02);
        assertEquals(decrypt(cipherText01), textToEncrypt01);
    }

    @Test
    public void testKickFromTresorAndTryDecryptText() {
        String tresor01 = createTresor();
        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);
        shareTresor(tresor01, user02);
        logoutTest();
        loginTest(user02, pass02);
        assertEquals(decrypt(cipherText01), textToEncrypt01);
        logoutTest();
        loginTest(user01, pass01);
        kickFromTresor(tresor01, user02);
        logoutTest();
        loginTest(user02, pass02);
        Assert.assertTrue(decrypt_noassert(cipherText01).isError());
    }

    @Test
    public void testInvitationLink() {
        String tresor01 = createTresor();
        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);
        String url = createInvitationLinkNoPasswordTest(user01, "", tresor01, "message");
        logoutTest();

        loginTest(user02, pass02);
        acceptInvitationLinkNoPasswordTest(url);
        assertEquals(decrypt(cipherText01), textToEncrypt01);
        acceptInvitationLinkNoPasswordFailedTest(url);
        acceptInvitationLinkFailedTest(url, "");
        logoutTest();

        loginTest(user01, pass01);
        kickFromTresor(tresor01, user02);
        revokeInvitationLink(tresor01, url);
        logoutTest();

        loginTest(user02, pass02);
        getInvitationLinkInfoFailedTest(url);
        decryptFailedTest(cipherText01);
        logoutTest();

        loginTest(user01, pass01);
        String password = "password";
        String urlPassword = createInvitationLinkPasswordTest(user01, "", tresor01, "message", password);
        logoutTest();

        loginTest(user02, pass02);
        getInvitationLinkInfoFailedTest(url);
        acceptInvitationLinkNoPasswordFailedTest(urlPassword);
        acceptInvitationLinkFailedTest(urlPassword, password + "0");
        decryptFailedTest(cipherText01);
        acceptInvitationLinkTest(urlPassword, password);
        assertEquals(decrypt(cipherText01), textToEncrypt01);
        acceptInvitationLinkFailedTest(urlPassword, password);
        logoutTest();

        loginTest(user01, pass01);
        kickFromTresor(tresor01, user02);
        logoutTest();

        loginTest(user02, pass02);
        decryptFailedTest(cipherText01);
        acceptInvitationLinkTest(urlPassword, password);
        assertEquals(decrypt(cipherText01), textToEncrypt01);
        logoutTest();
    }

    @Test
    public void testLoginWithRememberMe() {
        logoutTest();
        loginTest(user01, pass01, true);
        logoutTest(false);
        loginTest(user01);
        logoutTest(true);
        Assert.assertTrue(login_noassert(user01).isError());
        assertEquals(whoAmI(), "null");
    }

    @Test
    public void testLoginWithRememberMeAndChangePassword()  {
        logoutTest();
        loginTest(user01, pass01, true);
        changePasswordTestSmall(pass01);
        logoutTest(false);
        loginTest(user01);
        logoutTest();
    }


    private String registrationTest(String alias, String pass) {
        String userId = register(alias, pass);
        assertNotNull(userId);
        loginTest(userId, pass);
        logoutTest();
        return userId;
    }

    public void changePasswordTestWithLoggedIn(String user, String pass) {
        logoutTest();
        loginTest(user, pass);
        changePassword(getPasswordEditText(pass), getPasswordEditText(pass + "new"));
        assertEquals(whoAmI(), user);
        logoutTest();
        loginTest(user, pass + "new");
        changePassword(getPasswordEditText(pass + "new"), getPasswordEditText(pass));
        assertEquals(whoAmI(), user);
        logoutTest();
        loginTest(user, pass);
        logoutTest();
    }

    @NonNull
    private PasswordEditText getPasswordEditText(String pass) {
        return new PasswordEditText(targetContext, pass);
    }


    public void changePasswordTestWithLoggedOut(String user, String pass)  {
        logoutTest();
        changePassword(user, getPasswordEditText(pass), getPasswordEditText(pass + "new"));
        assertEquals(whoAmI(), "null");
        loginTest(user, pass + "new");
        logoutTest();
        changePassword(user, getPasswordEditText(pass + "new"), getPasswordEditText(pass));
        assertEquals(whoAmI(), "null");
        loginTest(user, pass);
        logoutTest();
    }

    public void changePasswordTestSmall(String pass)  {
        changePassword(getPasswordEditText(pass), getPasswordEditText(pass + "new"));
        changePassword(getPasswordEditText(pass + "new"), getPasswordEditText(pass));
    }


    private void loginTest(String user, String pass) {
        loginTest(user, pass, false);
    }

    private void loginIfNeededTest(String user, String pass) {
        if (!whoAmI().equals(user)) {
            logoutTest();
            loginTest(user, pass, false);
        }
    }

    private void loginTest(String user) {
        login(user);
        assertEquals(whoAmI(), user);
        saveToken();
    }

    private void loginTest(String user, String pass, boolean rememberMe) {
        login(user, pass, rememberMe);
        assertEquals(whoAmI(), user);
        saveToken();
    }

    private void saveToken() {
        String authCode = getAuthCode(adminApi.getClientId());
        assertNotNull(authCode);
        String token = loginWithCode(authCode);
        assertNotNull(token);
        adminApi.setToken(token);
    }

    private String encryptTest(String tresorId, String text) {
        String cipherText = encrypt(tresorId, text);
        String decryptedText = decrypt(cipherText);
        assertEquals(text, decryptedText);
        return cipherText;
    }

    private void storeProfileTest() {
        String profile = new JSONObject().put("test", "test").toString();
        setProfile(profile);
        assertEquals(profile, getProfile());
    }

    private void storePublicProfileTest(String userId) {
        String profile = new JSONObject().put("test", "test").toString();
        storePublicProfile(profile);
//        assertEquals(profile, getPublicProfile(userId));
    }

    private void storeDataTest(String userId) {
        String tresor = createTresor();
        String id = "id" + userId;
        String data = new JSONObject().put("test", "test").toString();
        storeData(data, tresor, id);
//        assertEquals(data, fetchData(id));
    }

    private String createInvitationLinkNoPasswordTest(String userId, String linkBase, String tresorId, String message) {
        String url = createInvitationLinkNoPassword(linkBase, tresorId, message);
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        assertEquals(invitationLinkInfo.getMessage(), message);
        assertEquals(invitationLinkInfo.getCreatorUserId(), userId);
        Assert.assertFalse(invitationLinkInfo.getPasswordProtected());
        return url;
    }

    private String createInvitationLinkPasswordTest(String userId, String linkBase, String tresorId, String message, String password) {
        String url = createInvitationLink(linkBase, tresorId, message, password);
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        assertEquals(invitationLinkInfo.getMessage(), message);
        assertEquals(invitationLinkInfo.getCreatorUserId(), userId);
        Assert.assertTrue(invitationLinkInfo.getPasswordProtected());
        return url;
    }

    private String createInvitationLinTest(String userId, String linkBase, String tresorId, String message, String password) {
        String url = createInvitationLink(linkBase, tresorId, message, password);
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        assertEquals(invitationLinkInfo.getMessage(), message);
        assertEquals(invitationLinkInfo.getCreatorUserId(), userId);
        Assert.assertTrue(invitationLinkInfo.getPasswordProtected());
        return url;
    }

    private void acceptInvitationLinkNoPasswordTest(String url) {
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        acceptInvitationLinkNoPassword(invitationLinkInfo.getToken());
    }

    private void acceptInvitationLinkNoPasswordFailedTest(String url) {
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        Response<String, ResponseZerokitError> response = acceptInvitationLinkNoPassword_noassert(invitationLinkInfo.getToken());
        Assert.assertTrue(response.isError());
    }

    private ResponseZerokitInvitationLinkInfo getInvitationLinkInfoTest(String url) {
        return getInvitationLinkInfo(url.substring(1));
    }

    private void getInvitationLinkInfoFailedTest(String url) {
        Assert.assertTrue(getInvitationLinkInfo_noassert(url.substring(1)).isError());
    }

    private void acceptInvitationLinkTest(String url, String password) {
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        acceptInvitationLink(invitationLinkInfo.getToken(), password);
    }

    private void acceptInvitationLinkFailedTest(String url, String password) {
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        Response<String, ResponseZerokitError> response = acceptInvitationLink_noassert(invitationLinkInfo.getToken(), password);
        Assert.assertTrue(response.isError());
    }

    private void logoutTest() {
        logout();
        assertEquals(whoAmI(), "null");
        adminApi.clearToken();
    }

    private void logoutTest(boolean deleteRememberMe) {
        logout(deleteRememberMe);
        assertEquals(whoAmI(), "null");
        adminApi.clearToken();
    }

    private void decryptFailedTest(String textToDecrypt) {
        Assert.assertTrue(zerokit.decrypt(textToDecrypt).execute().isError());
    }

    ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////

    public void assertFalse(boolean condition, ResponseZerokitError zerokitError) {
        Assert.assertFalse(condition ? zerokitError.toString() : "", condition);
    }

    public void assertFalse(boolean condition, ResponseAdminApiError adminApiError) {
        Assert.assertFalse(condition ? adminApiError.toString() : "", condition);
    }

    public void assertTrue(boolean condition, ResponseZerokitError adminApiError) {
        Assert.assertTrue(condition ? adminApiError.toString() : "", condition);
    }

    public void assertTrue(boolean condition, ResponseAdminApiError adminApiError) {
        Assert.assertTrue(condition ? adminApiError.toString() : "", condition);
    }

    private void login(String username, String password) {
        login(username, password, false);
    }

    private void login(final String username, final String password, final boolean rememberMe) {
        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Response<ResponseZerokitLogin, ResponseZerokitError> response = zerokit.login(username, getPasswordEditText(password), rememberMe).execute();
                assertFalse(response.isError(), response.getError());
            }
        });
    }

    private Response<ResponseZerokitLogin, ResponseZerokitError> login_noassert(final String username, final String password){
        return login_noassert(username, password, false);
    }

    private Response<ResponseZerokitLogin, ResponseZerokitError> login_noassert(final String username, final String password, final boolean rememberMe) {
        final Holder<Response<ResponseZerokitLogin, ResponseZerokitError>> response = new Holder<>();
        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                response.t = zerokit.login(username, getPasswordEditText(password), rememberMe).execute();
            }
        });
        return response.t;
    }


    private void login(String username) {
        Response<String, ResponseZerokitError> response = zerokit.login(username).execute();
        assertFalse(response.isError(), response.getError());
    }

    private String loginWithCode(String code) {
        Response<ResponseAdminApiLoginByCode, ResponseAdminApiError> response = adminApi.login(code).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult().getId();
    }

    private Response<String, ResponseZerokitError> login_noassert(String username) {
        return zerokit.login(username).execute();
    }

    private String getUserId(String alias) {
        Response<String, ResponseAdminApiError> response = adminApi.getUserId(alias).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult();
    }

    private void storePublicProfile(String publicProfile) {
        Response<Void, ResponseAdminApiError> response = adminApi.storePublicProfile(publicProfile).execute();
        assertFalse(response.isError(), response.getError());
    }

    private String getPublicProfile(String userId) {
        Response<String, ResponseAdminApiError> response = adminApi.getPublicProfile(userId).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult();
    }

    private void setProfile(String profile) {
        Response<String, ResponseAdminApiError> response = adminApi.setProfile(profile).execute();
        assertFalse(response.isError(), response.getError());
    }

    private String getProfile() {
        Response<String, ResponseAdminApiError> response = adminApi.getProfile().execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult();
    }

    private void storeData(String data, String tresorId, String id) {
        Response<Void, ResponseAdminApiError> response = adminApi.storeData(tresorId, id, data).execute();
        assertFalse(response.isError(), response.getError());
    }

    private String fetchData(String id) {
        Response<String, ResponseAdminApiError> response = adminApi.fetchData(id).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult();
    }

    private String register(String alias, String password) {
        final Response<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> responseInit = adminApi.initReg(alias, new JSONObject()
                .put("autoValidate", true)
                .put("canCreateTresor", true)
                .toString()).execute();
        assertFalse(responseInit.isError(), responseInit.getError());
        ResponseAdminApiInitUserRegistration resultInit = responseInit.getResult();
        final Response<ResponseZerokitRegister, ResponseZerokitError> responseRegister = zerokit.register(resultInit.getUserId(), resultInit.getRegSessionId(), password.getBytes()).execute();
        assertFalse(responseRegister.isError(), responseRegister.getError());
        Response<Void, ResponseAdminApiError> responseValidateUser = adminApi.finishReg(resultInit.getUserId(), responseRegister.getResult().getRegValidationVerifier()).execute();
        assertFalse(responseValidateUser.isError(), responseValidateUser.getError());
        return resultInit.getUserId();
    }


    private void logout() {
        Response<String, ResponseZerokitError> response = zerokit.logout().execute();
        assertFalse(response.isError(), response.getError());
    }

    private void logout(boolean deleteRememberMe) {
        Response<String, ResponseZerokitError> response = zerokit.logout(deleteRememberMe).execute();
        Assert.assertFalse(response.isError());
    }

    private String whoAmI() {
        Response<String, ResponseZerokitError> response = zerokit.whoAmI().execute();
        Assert.assertFalse(response.isError());
        return response.getResult();
    }

    private String getAuthCode(String clientId) {
        Response<IdentityTokens, ResponseZerokitError> response = zerokit.getIdentityTokens(clientId).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult().getAuthorizationCode();
    }


    private String createTresor() {
        Response<String, ResponseZerokitError> response = zerokit.createTresor().execute();
        Assert.assertFalse(response.isError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.createdTresor(response.getResult()).execute();
        assertFalse(responseApprove.isError(), responseApprove.getError());
        return response.getResult();
    }

    private String decrypt(String textToDecrypt) {
        Response<String, ResponseZerokitError> response = zerokit.decrypt(textToDecrypt).execute();
        Assert.assertFalse(response.isError());
        return response.getResult();
    }

    private Response<String, ResponseZerokitError> decrypt_noassert(String textToDecrypt) {
        return zerokit.decrypt(textToDecrypt).execute();
    }

    private String encrypt(String tresorId, String textToEncrypt) {
        Response<String, ResponseZerokitError> response = zerokit.encrypt(tresorId, textToEncrypt).execute();
        Assert.assertFalse(response.isError());
        return response.getResult();
    }


    private ResponseZerokitInvitationLinkInfo getInvitationLinkInfo(String secret) {
        Response<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> response = getInvitationLinkInfo_noassert(secret);
        assertFalse(response.isError(), response.getError());
        return response.getResult();
    }
    private ResponseZerokitPasswordStrength getPasswordStrengthTest(String password) {
        Response<ResponseZerokitPasswordStrength, ResponseZerokitError> response = zerokit.getPasswordStrength(getPasswordEditText(password)).execute();
        Assert.assertFalse(response.isError());
        assertNotNull(response.getResult().getCrack_times_seconds());
        assertNotNull(response.getResult().getFeedback());
        assertNotNull(response.getResult().getGuesses_log10());
        assertNotNull(response.getResult().getScore());
        assertNotNull(response.getResult().getLength());
        return response.getResult();
    }

    private Response<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> getInvitationLinkInfo_noassert(String secret) {
        return  zerokit.getInvitationLinkInfo(secret).execute();
    }

    private void acceptInvitationLinkNoPassword(String token) {
        Response<String, ResponseZerokitError> response = acceptInvitationLinkNoPassword_noassert(token);
        assertFalse(response.isError(), response.getError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.acceptedInvitationLink(response.getResult()).execute();
        Assert.assertFalse(responseApprove.isError());
    }

    private Response<String, ResponseZerokitError> acceptInvitationLinkNoPassword_noassert(String token) {
        return zerokit.acceptInvitationLinkNoPassword(token).execute();
    }

    private void acceptInvitationLink(String token, String password) {
        Response<String, ResponseZerokitError> response = acceptInvitationLink_noassert(token, password);
        assertFalse(response.isError(), response.getError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.acceptedInvitationLink(response.getResult()).execute();
        Assert.assertFalse(responseApprove.isError());
    }

    private Response<String, ResponseZerokitError> acceptInvitationLink_noassert(String token, String password) {
        return zerokit.acceptInvitationLink(token, getPasswordEditText(password)).execute();
    }

    private void changePassword(String userId, String oldPassword, String newPassword) {
        Response<ResponseZerokitChangePassword, ResponseZerokitError> response = zerokit.changePassword(userId, oldPassword.getBytes(), newPassword.getBytes()).execute();
        assertFalse(response.isError(), response.getError());
    }

    private void changePassword(String oldPassword, String newPassword) {
        Response<ResponseZerokitChangePassword, ResponseZerokitError> response = zerokit.changePassword(oldPassword.getBytes(), newPassword.getBytes()).execute();
        assertFalse(response.isError(), response.getError());
    }

    private void changePassword(final PasswordEditText oldPassword, final PasswordEditText newPassword) {
        changePassword("", oldPassword, newPassword);
    }

    private void changePassword(final String userId, final PasswordEditText oldPassword, final PasswordEditText newPassword) {
        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Response<ResponseZerokitChangePassword, ResponseZerokitError> response = TextUtils.isEmpty(userId) ? zerokit.changePassword(oldPassword, newPassword).execute() : zerokit.changePassword(userId, oldPassword, newPassword).execute();
                assertFalse(response.isError(), response.getError());
            }
        });
    }

    private void shareTresor(String tresorId, String userId) {
        Response<String, ResponseZerokitError> response = zerokit.shareTresor(tresorId, userId).execute();
        Assert.assertFalse(response.isError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.sharedTresor(response.getResult()).execute();
        assertFalse(responseApprove.isError(), responseApprove.getError());
    }

    private void kickFromTresor(String tresorId, String userId) {
        Response<String, ResponseZerokitError> response = zerokit.kickFromTresor(tresorId, userId).execute();
        Assert.assertFalse(response.isError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.kickedUser(response.getResult()).execute();
        assertFalse(responseApprove.isError(), responseApprove.getError());
    }

    private String createInvitationLinkNoPassword(String linkBase, String tresorId, String message) {
        Response<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.createInvitationLinkNoPassword(linkBase, tresorId, message).execute();
        Assert.assertFalse(response.isError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.createdInvitationLink(response.getResult().getId()).execute();
        Assert.assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

    private String createInvitationLink(final String linkBase, final String tresorId, final String message, final String password) {
        Response<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.createInvitationLink(linkBase, tresorId, message, getPasswordEditText(password)).execute();
        Assert.assertFalse(response.isError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.createdInvitationLink(response.getResult().getId()).execute();
        Assert.assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

    private void revokeInvitationLink(final String tresorId, final String url) {
        Response<String, ResponseZerokitError> response = zerokit.revokeInvitationLink( tresorId, url.substring(1)).execute();
        assertFalse(response.isError(), response.getError());
        Response<Void, ResponseAdminApiError> responseApprove = adminApi.revokedInvitationLink(response.getResult()).execute();
        Assert.assertFalse(responseApprove.isError());
    }


        @Test
    public void testThread() {
        threadTest(new Handler(Looper.getMainLooper()));
        HandlerThread handlerThread = new HandlerThread("Test Background Thread");
        handlerThread.start();
        threadTest(new Handler(handlerThread.getLooper()));
    }

        @Test
    public void testThreadHard() {
        threadTestHard(new Handler(Looper.getMainLooper()));
        HandlerThread handlerThread = new HandlerThread("Test Background Thread");
        handlerThread.start();
        threadTestHard(new Handler(handlerThread.getLooper()));
    }

    public void threadTestHard(Handler handler) {
        final CountDownLatch signal = new CountDownLatch(100);
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++)
                    zerokit.whoAmI().enqueue(new Action<String>() {

                        @Override
                        public void call(String s) {
                            signal.countDown();
                        }
                    }, new Action<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            Assert.assertTrue(false);
                        }
                    });
            }
        });
        if (signal.getCount() > 0)
            try {
                signal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    public void threadTest(final Handler handler) {
        final CountDownLatch signal = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Response<String, ResponseZerokitError> response = zerokit.whoAmI().execute();
                Assert.assertFalse(response.isError());
                zerokit.whoAmI().enqueue(new Action<String>() {
                    @Override
                    public void call(String s) {
                        Assert.assertEquals(handler.getLooper(), Looper.myLooper());
                        Response<String, ResponseZerokitError> response = zerokit.whoAmI().execute();
                        Assert.assertFalse(response.isError());
                        zerokit.whoAmI().enqueue(new Action<String>() {
                            @Override
                            public void call(String s) {
                                signal.countDown();
                            }
                        }, new Action<ResponseZerokitError>() {
                            @Override
                            public void call(ResponseZerokitError responseZerokitError) {
                                Assert.assertTrue(false);
                            }
                        });
                    }
                }, new Action<ResponseZerokitError>() {
                    @Override
                    public void call(ResponseZerokitError responseZerokitError) {
                        Assert.assertTrue(false);
                    }
                });
            }
        });
        if (signal.getCount() > 0)
            try {
                signal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }



}
