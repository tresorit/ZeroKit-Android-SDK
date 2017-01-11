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
import com.tresorit.zerokit.extension.Result;
import com.tresorit.zerokit.response.ResponseZerokitCreateInvitationLink;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitInvitationLinkInfo;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokit.response.ResponseZerokitRegister;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Properties;

import static com.tresorit.zerokit.Zerokit.API_ROOT;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testChangePasswordLoggedIn(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);

        loginTest(user01, pass01);

        changePasswordTestWithLoggedIn(user01, pass01);

        logoutTest();
    }

    @Test
    public void testChangePasswordLoggedOut(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);

        loginTest(user01, pass01);

        changePasswordTestWithLoggedOut(user01, pass01);

        logoutTest();
    }

    @Test
    public void testCreateTresorAndEncryptText(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);

        loginTest(user01, pass01);

        String tresor01 = createTresor();

        String textToEncrypt01 = "textToEncrypt01";
        encryptTest(tresor01, textToEncrypt01);

        logoutTest();
    }

    @Test
    public void testShareTresorAndDecryptText(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

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
    public void testKickFromTresorAndTryDecryptText(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);
        String pass02 = USER_02_PASS;
        String user02 = registrationTest(USER_02_ALIAS, pass02);

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

        assertTrue(decrypt_noassert(cipherText01).isError());

        logoutTest();
    }

    @Test
    public void testShareTresorWithLinkAndDecryptText(){
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

    @Test
    public void testKickFromLinkSharedTresorAndTryDecryptText(){
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

        assertTrue(decrypt_noassert(cipherText01).isError());

        logoutTest();
    }

    @Test
    public void testShareTresorWithLinkAndPasswordAndDecryptText(){
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

    @Test
    public void testKickFromLinkSharedTresorWithPasswordAndTryDecryptText(){
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

        assertTrue(decrypt_noassert(cipherText01).isError());

        logoutTest();
    }

    @Test
    public void testLoginWithRememberMe(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);

        loginTest(user01, pass01, true);

        logoutTest(false);

        loginTest(user01);

        logoutTest(true);

        assertTrue(login_noassert(user01).isError());

        assertEquals(whoAmI(), "null");
    }

    @Test
    public void testLoginWithRememberMeAndChangePassword(){
        String pass01 = USER_01_PASS;
        String user01 = registrationTest(USER_01_ALIAS, pass01);

        loginTest(user01, pass01, true);

        changePasswordTestSmall(user01, pass01);

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

    public void changePasswordTestSmall(String user, String pass) {
        changePassword( pass, pass + "new");
        changePassword( pass + "new", pass);
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
        assertFalse(invitationLinkInfo.getPasswordProtected());
        return url;
    }

    private String createInvitationLinTest(String userId, String linkBase, String tresorId, String message, String password) {
        String url = createInvitationLink(linkBase, tresorId, message, password);
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        assertEquals(invitationLinkInfo.getMessage(), message);
        assertEquals(invitationLinkInfo.getCreatorUserId(), userId);
        assertTrue(invitationLinkInfo.getPasswordProtected());
        return url;
    }

    private void acceptInvitationLinkNoPasswordTest(String url){
        ResponseZerokitInvitationLinkInfo invitationLinkInfo = getInvitationLinkInfo(url.substring(1));
        acceptInvitationLinkNoPassword(invitationLinkInfo.getToken());
    }

    private void acceptInvitationLinkTest(String url, String password){
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
        Result<ResponseZerokitLogin, ResponseZerokitError> response = zerokit.sync().login(username, password, rememberMes);
        assertTrue(!response.isError());
    }


    private void login(String username) {
        Result<String, ResponseZerokitError> response = zerokit.sync().login(username);
        assertTrue(!response.isError());
    }

    private Result<String, ResponseZerokitError> login_noassert(String username) {
        return zerokit.sync().login(username);
    }

    private String register(String alias, String password) {
        Result<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> responseInit = adminApi.sync().initUserRegistration();
        assertFalse(responseInit.isError());
        ResponseAdminApiInitUserRegistration resultInit = responseInit.getResult();
        Result<ResponseZerokitRegister, ResponseZerokitError> responseRegister = zerokit.sync().register(resultInit.getUserId(), resultInit.getRegSessionId(), password.getBytes());
        assertFalse(responseRegister.isError());
        ResponseZerokitRegister resultRegister = responseRegister.getResult();
        Result<String, ResponseAdminApiError> responseValidateUser = adminApi.sync().validateUser(resultInit.getUserId(), resultInit.getRegSessionId(), resultInit.getRegSessionVerifier(), resultRegister.getRegValidationVerifier(), alias);
        assertFalse(responseValidateUser.isError());
        return resultInit.getUserId();
    }


    private void logout() {
        Result<String, ResponseZerokitError> response = zerokit.sync().logout();
        assertFalse(response.isError());
    }

    private void logout(boolean deleteRememberMe) {
        Result<String, ResponseZerokitError> response = zerokit.sync().logout(deleteRememberMe);
        assertFalse(response.isError());
    }

    private String whoAmI() {
        Result<String, ResponseZerokitError> response = zerokit.sync().whoAmI();
        assertFalse(response.isError());
        return response.getResult();
    }


    private String createTresor() {
        Result<String, ResponseZerokitError> response = zerokit.sync().createTresor();
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveTresorCreation(response.getResult());
        assertFalse(responseApprove.isError());
        return response.getResult();
    }

    private String decrypt(String textToDecrypt) {
        Result<String, ResponseZerokitError> response = zerokit.sync().decrypt(textToDecrypt);
        assertFalse(response.isError());
        return response.getResult();
    }

    private Result<String, ResponseZerokitError> decrypt_noassert(String textToDecrypt) {
        return zerokit.sync().decrypt(textToDecrypt);
    }

    private String encrypt(String tresorId, String textToEncrypt) {
        Result<String, ResponseZerokitError> response = zerokit.sync().encrypt(tresorId, textToEncrypt);
        assertFalse(response.isError());
        return response.getResult();
    }


    private ResponseZerokitInvitationLinkInfo getInvitationLinkInfo(String secret) {
        Result<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> response = zerokit.sync().getInvitationLinkInfo(secret);
        assertFalse(response.isError());
        return response.getResult();
    }

    private void acceptInvitationLinkNoPassword(String token) {
        Result<String, ResponseZerokitError> response = zerokit.sync().acceptInvitationLinkNoPassword(token);
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveInvitationLinkAcception(response.getResult());
        assertFalse(responseApprove.isError());
    }

    private void acceptInvitationLink(String token, String password) {
        Result<String, ResponseZerokitError> response = zerokit.sync().acceptInvitationLink(token, password.getBytes());
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveInvitationLinkAcception(response.getResult());
        assertFalse(responseApprove.isError());
    }

    private void changePassword(String userId, String oldPassword, String newPassword) {
        Result<String, ResponseZerokitError> response = zerokit.sync().changePassword(userId, oldPassword.getBytes(), newPassword.getBytes());
        assertFalse(response.isError());
    }

    private void changePassword(String oldPassword, String newPassword) {
        Result<String, ResponseZerokitError> response = zerokit.sync().changePassword(oldPassword.getBytes(), newPassword.getBytes());
        assertFalse(response.isError());
    }

    private void shareTresor(String tresorId, String userId) {
        Result<String, ResponseZerokitError> response = zerokit.sync().shareTresor(tresorId, userId);
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveShare(response.getResult());
        assertFalse(responseApprove.isError());
    }

    private void kickFromTresor(String tresorId, String userId) {
        Result<String, ResponseZerokitError> response = zerokit.sync().kickFromTresor(tresorId, userId);
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveKick(response.getResult());
        assertFalse(responseApprove.isError());
    }

    private String createInvitationLinkNoPassword(String linkBase, String tresorId, String message) {
        Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.sync().createInvitationLinkNoPassword(linkBase, tresorId, message);
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveCreateInvitationLink(response.getResult().getId());
        assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

    private String createInvitationLink(final String linkBase, final String tresorId, final String message, final String password) {
        Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> response = zerokit.sync().createInvitationLink(linkBase, tresorId, message, password.getBytes());
        assertFalse(response.isError());
        Result<String, ResponseAdminApiError> responseApprove = adminApi.sync().approveCreateInvitationLink(response.getResult().getId());
        assertFalse(responseApprove.isError());
        return response.getResult().getUrl();
    }

}
