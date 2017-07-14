package com.tresorit.zerokitsdk.activity;


import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.EditText;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokitsdk.R;
import com.tresorit.zerokitsdk.ZerokitApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;

@RunWith(AndroidJUnit4.class)
public class SampleAppTest {

    @Rule
    public ActivityTestRule<RootActivity> mActivityTestRule = new ActivityTestRule<>(RootActivity.class);


    @Before
    public void setUp() {
        Espresso.registerIdlingResources(Zerokit.getInstance().getIdlingResource());
        Espresso.registerIdlingResources(((ZerokitApplication) mActivityTestRule.getActivity().getApplication()).component().adminApi().getIdlingResource());
    }


    @After
    public void tearDown() {
        Espresso.unregisterIdlingResources(Zerokit.getInstance().getIdlingResource());
        Espresso.unregisterIdlingResources(((ZerokitApplication) mActivityTestRule.getActivity().getApplication()).component().adminApi().getIdlingResource());
    }

    private static final String USER_01_ALIAS = String.format("test-user01-%s", UUID.randomUUID());
    private static final String USER_02_ALIAS = String.format("test-user02-%s", UUID.randomUUID());
    private static final String USER_01_PASS = "password01";
    private static final String USER_02_PASS = "password02";

    @Test
    public void mainTest() throws InterruptedException {
        String pass01 = USER_01_PASS;
        String user01 = USER_01_ALIAS;
        String pass02 = USER_02_PASS;
        String user02 = USER_02_ALIAS;

        Thread.sleep(5000);
        pressBack();
        Thread.sleep(500);
        signUp(user01, pass01);
        signUp(user02, pass02);
        login(user01, pass01);
        createTresor();
        String encryptedText = encrypt("I want to encrypt this");
        share(user02);
        logout();
        pressBack();
        login(user02, pass02);
        decrypt(encryptedText, "I want to encrypt this");
        logout();
    }

    private void decrypt(String encryptedText, String expectedText) throws InterruptedException {
        onView(allOf(withId(R.id.tab_decrypt), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.editText4), isDisplayed())).perform(replaceText(encryptedText));
        onView(allOf(withId(R.id.button3), withText("Decrypt"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.textView5), isDisplayed())).check(matches(withText(expectedText)));
    }


    private String encrypt(String text) throws InterruptedException {
        final String[] result = new String[1];

        onView(withId(R.id.editText4)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
        onView(allOf(withId(R.id.button2), withText("Encrypt"))).perform(scrollTo(), click());
        onView(withId(R.id.editText5)).check(new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noViewFoundException) {
                result[0] = String.valueOf(((EditText) view).getText());
            }
        });
        onView(allOf(withId(R.id.button3), withText("Test Decrypt"))).perform(scrollTo(), click());
        onView(withId(R.id.textView5)).check(matches(withText(text)));
        onView(allOf(withId(R.id.button4), withText("Copy"))).perform(scrollTo(), click());

        return result[0];
    }

    private void share(String userIdOrName) throws InterruptedException {
        onView(allOf(withId(R.id.pager), isDisplayed())).perform(swipeLeft());
        onView(allOf(withId(R.id.editText6), isDisplayed())).perform(replaceText(userIdOrName), closeSoftKeyboard());
        Thread.sleep(500);
        onView(allOf(withId(R.id.button2), withText("Share"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.textView7), isDisplayed())).check(matches(allOf(isDisplayed(), withText(startsWith("Shared with")))));
        onView(allOf(withId(R.id.imageView), isDisplayed())).check(matches(isDisplayed()));
    }

    private void logout() throws InterruptedException {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(allOf(withId(R.id.title), withText("Logout"), isDisplayed())).perform(click());
    }

    private void createTresor() throws InterruptedException {
        onView(allOf(withId(R.id.button2), withText("Create Tresor"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.imageView), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.textView7), isDisplayed())).check(matches(allOf(isDisplayed(), withText(startsWith("Tresor Id:")))));
        Thread.sleep(2000);
    }


    private void login(String userName, String password) throws InterruptedException {
        onView(allOf(withId(R.id.tab_signin), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.editText), isDescendantOfA(withId(R.id.signInFragment)))).perform(scrollTo(), click(), typeText(userName), closeSoftKeyboard());
        onView(allOf(withId(R.id.editText2), isDescendantOfA(withId(R.id.signInFragment)))).perform(scrollTo(), typeText(password), closeSoftKeyboard());
        onView(allOf(withId(R.id.button), withText("Login"), withParent(allOf(withId(R.id.constraintLayout), withParent(withId(R.id.signInFragment)))))).perform(scrollTo(), click());
        onView(allOf(withId(R.id.textView2), isDisplayed())).check(matches(withText("Step 1: Create a Tresor")));
    }

    private void signUp(String userName, String password) throws InterruptedException {
        onView(allOf(withId(R.id.tab_signup), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.editText), isDisplayed())).perform(scrollTo(), click(), typeText(userName), closeSoftKeyboard());
        onView(allOf(withId(R.id.editText2), isDisplayed())).perform(scrollTo(), click(), typeText(password), closeSoftKeyboard());
        onView(allOf(withId(R.id.editText3), isDisplayed())).perform(scrollTo(), click(), typeText(password), closeSoftKeyboard());
        onView(allOf(withId(R.id.button), withText("Sign Up"), withParent(allOf(withId(R.id.constraintLayout), withParent(withId(R.id.signUpFragment)))))).perform(scrollTo(), click());
        onView(withText("Successful sign up")).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        Thread.sleep(5000);
    }

}
