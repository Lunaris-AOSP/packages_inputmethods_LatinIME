/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.inputmethod.latin.utils.ManagedProfileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AccountsSettingsFragmentTests {
    private static final String FRAG_NAME = AccountsSettingsFragment.class.getName();
    private static final long TEST_TIMEOUT_MILLIS = 5000;

    @Mock private ManagedProfileUtils mManagedProfileUtils;

    private TestFragmentActivity mActivity;
    private TestFragmentActivity getActivity() {
        return mActivity;
    }

    @Before
    public void setUp() throws Exception {
        // Initialize the mocks.
        MockitoAnnotations.initMocks(this);
        ManagedProfileUtils.setTestInstance(mManagedProfileUtils);

        final Intent intent = new Intent()
                .setAction(Intent.ACTION_MAIN)
                .setClass(InstrumentationRegistry.getTargetContext(), TestFragmentActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(TestFragmentActivity.EXTRA_SHOW_FRAGMENT, FRAG_NAME);
        mActivity = (TestFragmentActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
    }

    @After
    public void tearDown() throws Exception {
        ManagedProfileUtils.setTestInstance(null);
        mActivity = null;
    }

    @Test
    public void testEmptyAccounts() {
        final AccountsSettingsFragment fragment =
                (AccountsSettingsFragment) getActivity().mFragment;
        try {
            fragment.createAccountPicker(new String[0], null, null /* listener */);
            fail("Expected IllegalArgumentException, never thrown");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static class DialogHolder {
        AlertDialog mDialog;
        DialogHolder() {}
    }

    @Test
    public void testMultipleAccounts_noSettingsForManagedProfile() {
        when(mManagedProfileUtils.hasWorkProfile(any(Context.class))).thenReturn(true);

        final AccountsSettingsFragment fragment =
                (AccountsSettingsFragment) getActivity().mFragment;
        final AlertDialog dialog = initDialog(fragment, null).mDialog;
        final ListView lv = dialog.getListView();

        // Nothing to check/uncheck.
        assertNull(fragment.findPreference(AccountsSettingsFragment.PREF_ACCCOUNT_SWITCHER));
    }

    @Test
    public void testMultipleAccounts_noCurrentAccount() {
        when(mManagedProfileUtils.hasWorkProfile(any(Context.class))).thenReturn(false);

        final AccountsSettingsFragment fragment =
                (AccountsSettingsFragment) getActivity().mFragment;
        final AlertDialog dialog = initDialog(fragment, null).mDialog;
        final ListView lv = dialog.getListView();

        // The 1st account should be checked by default.
        assertEquals("checked-item", 0, lv.getCheckedItemPosition());
        // There should be 4 accounts in the list.
        assertEquals("count", 4, lv.getCount());
        // The sign-out button shouldn't exist
        assertEquals(View.GONE,
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).getVisibility());
        assertEquals(View.VISIBLE,
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility());
        assertEquals(View.VISIBLE,
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).getVisibility());
    }

    @Test
    public void testMultipleAccounts_currentAccount() {
        when(mManagedProfileUtils.hasWorkProfile(any(Context.class))).thenReturn(false);

        final AccountsSettingsFragment fragment =
                (AccountsSettingsFragment) getActivity().mFragment;
        final AlertDialog dialog = initDialog(fragment, "3@example.com").mDialog;
        final ListView lv = dialog.getListView();

        // The 3rd account should be checked by default.
        assertEquals("checked-item", 2, lv.getCheckedItemPosition());
        // There should be 4 accounts in the list.
        assertEquals("count", 4, lv.getCount());
        // The sign-out button should be shown
        assertEquals(View.VISIBLE,
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).getVisibility());
        assertEquals(View.VISIBLE,
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility());
        assertEquals(View.VISIBLE,
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).getVisibility());
    }

    private DialogHolder initDialog(
            final AccountsSettingsFragment fragment,
            final String selectedAccount) {
        final DialogHolder dialogHolder = new DialogHolder();
        final CountDownLatch latch = new CountDownLatch(1);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog dialog = fragment.createAccountPicker(
                        new String[] {
                                "1@example.com",
                                "2@example.com",
                                "3@example.com",
                                "4@example.com"},
                        selectedAccount, null /* positiveButtonListner */);
                dialog.show();
                dialogHolder.mDialog = dialog;
                latch.countDown();
            }
        });

        try {
            latch.await(TEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            fail();
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return dialogHolder;
    }
}
