package com.tresorit.zerokitsdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.adminapi.response.ResponseAdminApiError;
import com.tresorit.adminapi.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Response;
import com.tresorit.zerokit.response.IdentityTokens;
import com.tresorit.zerokit.response.ResponseZerokitCreateInvitationLink;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitInvitationLinkInfo;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokit.response.ResponseZerokitRegister;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Properties;

import static com.tresorit.zerokit.Zerokit.API_ROOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTest {

    @Rule
    public final UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    @SuppressWarnings("WeakerAccess")
    Zerokit zerokit;
    private AdminApi adminApi;

    @Before
    public void init() throws Throwable {

        if (adminApi == null) {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            try {
                Properties properties = new Properties();
                properties.load(targetContext.getAssets().open("zerokit.properties"));
                adminApi = new AdminApi(properties.getProperty("adminuserid", ""), properties.getProperty("adminkey", ""), targetContext.getPackageManager().getApplicationInfo(targetContext.getPackageName(), PackageManager.GET_META_DATA).metaData.getString(API_ROOT));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (zerokit == null) {
            zerokit = Zerokit.getInstance();
        }

        assertNotNull(adminApi);
        assertNotNull(zerokit);
        logout();
        assertEquals(whoAmI(), "null");
    }


    private static final String USER_01_ALIAS = "User01";
    private static final String USER_02_ALIAS = "User02";
    private static final String USER_01_PASS = "Password01";
    private static final String USER_02_PASS = "Password02";

    private String getUserName(String userName) {
        return userName;
    }

    @Test
    public void testLogin() {
        String pass01 = USER_01_PASS;
        String user01 = getUserName(USER_01_ALIAS);
        user01 = registrationTest(user01, pass01);

        loginTest(user01, pass01);

        logoutTest();
    }

    @Test
    public void testChangePasswordLoggedIn() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);

        loginTest(user01, pass01);

        changePasswordTestWithLoggedIn(user01, pass01);

        logoutTest();
    }

    @Test
    public void testChangePasswordLoggedOut() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);

        loginTest(user01, pass01);

        changePasswordTestWithLoggedOut(user01, pass01);

        logoutTest();
    }

    @Test
    public void testCreateTresorAndEncryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        encryptTest(tresor01, textToEncrypt01);

        logoutTest();
    }

    @Test
    public void testShareTresorAndDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(getUserName(USER_02_ALIAS), pass02);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);

        shareTresor(tresor01, user02);

        logoutTest();

        loginTest(user02, pass02);

        assertEquals(decrypt(cipherText01), textToEncrypt01);

        logoutTest();
    }

    @Test
    public void testKickFromTresorAndTryDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(getUserName(USER_02_ALIAS), pass02);

        loginTest(user01, pass01);

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

        logoutTest();
    }

    public void testShareTresorWithLinkAndDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);

        String url = createInvitationLinkNoPasswordTest(user01, "", tresor01, "message");

        logoutTest();

        loginTest(user02, pass02);

        acceptInvitationLinkNoPasswordTest(url);

        assertEquals(decrypt(cipherText01), textToEncrypt01);

        logoutTest();
    }

    public void testKickFromLinkSharedTresorAndTryDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);

        String url = createInvitationLinkNoPasswordTest(user01, "", tresor01, "message");

        logoutTest();

        loginTest(user02, pass02);

        acceptInvitationLinkNoPasswordTest(url);

        assertEquals(decrypt(cipherText01), textToEncrypt01);

        logoutTest();

        loginTest(user01, pass01);

        kickFromTresor(tresor01, user02);

        logoutTest();

        loginTest(user02, pass02);

        Assert.assertTrue(decrypt_noassert(cipherText01).isError());

        logoutTest();
    }

    public void testShareTresorWithLinkAndPasswordAndDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);

        String url = createInvitationLinTest(user01, "", tresor01, "message", "password");

        logoutTest();

        loginTest(user02, pass02);

        acceptInvitationLinkTest(url, "password");

        assertEquals(decrypt(cipherText01), textToEncrypt01);

        logoutTest();
    }

    public void testKickFromLinkSharedTresorWithPasswordAndTryDecryptText() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        String cipherText01 = encryptTest(tresor01, textToEncrypt01);

        String url = createInvitationLinTest(user01, "", tresor01, "message", "password");

        logoutTest();

        loginTest(user02, pass02);

        acceptInvitationLinkTest(url, "password");

        assertEquals(decrypt(cipherText01), textToEncrypt01);

        logoutTest();

        loginTest(user01, pass01);

        kickFromTresor(tresor01, user02);

        logoutTest();

        loginTest(user02, pass02);

        Assert.assertTrue(decrypt_noassert(cipherText01).isError());

        logoutTest();
    }

    @Test
    public void testLoginWithRememberMe() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);

        loginTest(user01, pass01, true);

        logoutTest(false);

        loginTest(user01);

        logoutTest(true);

        Assert.assertTrue(login_noassert(user01).isError());

        assertEquals(whoAmI(), "null");
    }

    @Test
    public void testLoginWithRememberMeAndChangePassword() {
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(getUserName(USER_01_ALIAS), pass01);

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
        changePassword(pass, pass + "new");
        assertEquals(whoAmI(), user);
        logoutTest();
        loginTest(user, pass + "new");
        changePassword(user, pass + "new", pass);
        assertEquals(whoAmI(), user);
        logoutTest();
        loginTest(user, pass);
        logoutTest();
    }


    public void changePasswordTestWithLoggedOut(String user, String pass) {
        logoutTest();
        changePassword(user, pass, pass + "new");
        assertEquals(whoAmI(), "null");
        loginTest(user, pass + "new");
        logoutTest();
        changePassword(user, pass + "new", pass);
        assertEquals(whoAmI(), "null");
        loginTest(user, pass);
        logoutTest();
    }

    public void changePasswordTestSmall(String pass) {
        changePassword(pass, pass + "new");
        changePassword(pass + "new", pass);
    }


    private void loginTest(String user, String pass) {
        loginTest(user, pass, false);
    }

    private void loginTest(String user) {
        login(user);
        assertEquals(whoAmI(), user);
    }

    private void loginTest(String user, String pass, boolean rememberMe) {
        login(user, pass, rememberMe);
        assertEquals(whoAmI(), user);
    }


    private String encryptTest(String tresorId, String text) {
        String cipherText = encrypt(tresorId, text);
        String decryptedText = decrypt(cipherText);
        assertEquals(text, decryptedText);
        return cipherText;
    }

    private String createInvitationLinkNoPasswordTest(String userId, String linkBase, String tresorId, String message) {
        String url = createInvitationLinkNoPassword(linkBase, tresorId, message);
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        assertEquals(invitationLinkInfo.getMessage(), message);
        assertEquals(invitationLinkInfo.getCreatorUserId(), userId);
        Assert.assertFalse(invitationLinkInfo.getPasswordProtected());
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

    private void acceptInvitationLinkTest(String url, String password) {
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        acceptInvitationLink(invitationLinkInfo.getToken(), password);
    }

    private void logoutTest() {
        logout();
        assertEquals(whoAmI(), "null");
    }

    private void logoutTest(boolean deleteRememberMe) {
        logout(deleteRememberMe);
        assertEquals(whoAmI(), "null");
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
        login(username, password.getBytes());
    }

    private void login(String username, String password, boolean rememberMe) {
        login(username, password.getBytes(), rememberMe);
    }

    private void login(String username, byte[] password) {
        login(username, password, false);
    }

    private void login(String username, byte[] password, boolean rememberMes) {
        Response<ResponseZerokitLogin, ResponseZerokitError> response = zerokit.login(username, password, rememberMes).execute();
        assertFalse(response.isError(), response.getError());
    }


    private void login(String username) {
        Response<String, ResponseZerokitError> response = zerokit.login(username).execute();
        assertFalse(response.isError(), response.getError());
    }


    private Response<String, ResponseZerokitError> login_noassert(String username) {
        return zerokit.login(username).execute();
    }

    private String register(String alias, String password) {
        Response<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> responseInit = adminApi.initUserRegistration().execute();
        assertFalse(responseInit.isError(),responseInit.getError());
        ResponseAdminApiInitUserRegistration resultInit = responseInit.getResult();
        Response<ResponseZerokitRegister, ResponseZerokitError> responseRegister = zerokit.register(resultInit.getUserId(), resultInit.getRegSessionId(), password.getBytes()).execute();
        assertFalse(responseRegister.isError(), responseRegister.getError());
        ResponseZerokitRegister resultRegister = responseRegister.getResult();
        Response<String, ResponseAdminApiError> responseValidateUser = adminApi.validateUser(resultInit.getUserId(), resultInit.getRegSessionId(), resultInit.getRegSessionVerifier(), resultRegister.getRegValidationVerifier(), alias).execute();
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
        Response<IdentityTokens, ResponseZerokitError> response = zerokit.getIdentityTokens(clientId, true).execute();
        assertFalse(response.isError(), response.getError());
        return response.getResult().getAuthorizationCode();
    }


    private String createTresor() {
        Response<String, ResponseZerokitError> response = zerokit.createTresor().execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveTresorCreation(response.getResult()).execute();
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
        Response<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> response = zerokit.getInvitationLinkInfo(secret).execute();
        Assert.assertFalse(response.isError());
        return response.getResult();
    }

    private void acceptInvitationLinkNoPassword(String token) {
        Response<String, ResponseZerokitError> response = zerokit.acceptInvitationLinkNoPassword(token).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveInvitationLinkAcception(response.getResult()).execute();
        Assert.assertFalse(responseApprove.isError());
    }

    private void acceptInvitationLink(String token, String password) {
        Response<String, ResponseZerokitError> response = zerokit.acceptInvitationLink(token, password.getBytes()).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveInvitationLinkAcception(response.getResult()).execute();
        Assert.assertFalse(responseApprove.isError());
    }

    private void changePassword(String userId, String oldPassword, String newPassword) {
        Response<String, ResponseZerokitError> response = zerokit.changePassword(userId, oldPassword.getBytes(), newPassword.getBytes()).execute();
        Assert.assertFalse(response.isError());
    }

    private void changePassword(String oldPassword, String newPassword) {
        Response<String, ResponseZerokitError> response = zerokit.changePassword(oldPassword.getBytes(), newPassword.getBytes()).execute();
        Assert.assertFalse(response.isError());
    }

    private void shareTresor(String tresorId, String userId) {
        Response<String, ResponseZerokitError> response = zerokit.shareTresor(tresorId, userId).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveShare(response.getResult()).execute();
        assertFalse(responseApprove.isError(), responseApprove.getError());
    }

    private void kickFromTresor(String tresorId, String userId) {
        Response<String, ResponseZerokitError> response = zerokit.kickFromTresor(tresorId, userId).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approvekick(response.getResult()).execute();
        assertFalse(responseApprove.isError(), responseApprove.getError());
    }

    private String createInvitationLinkNoPassword(String linkBase, String tresorId, String message) {
        Response<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.createInvitationLinkNoPassword(linkBase, tresorId, message).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveCreateInvitationLink(response.getResult().getId()).execute();
        Assert.assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

    private String createInvitationLink(final String linkBase, final String tresorId, final String message, final String password) {
        Response<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.createInvitationLink(linkBase, tresorId, message, password.getBytes()).execute();
        Assert.assertFalse(response.isError());
        Response<String, ResponseAdminApiError> responseApprove = adminApi.approveCreateInvitationLink(response.getResult().getId()).execute();
        Assert.assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

}
