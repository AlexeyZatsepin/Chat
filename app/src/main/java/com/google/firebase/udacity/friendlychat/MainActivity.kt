/**
 * Copyright Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast

import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

import java.util.ArrayList
import java.util.HashMap

class MainActivity : AppCompatActivity() {

    private var mMessageListView: ListView? = null
    private var mMessageAdapter: MessageAdapter? = null
    private var mProgressBar: ProgressBar? = null
    private var mPhotoPickerButton: ImageButton? = null
    private var mMessageEditText: EditText? = null
    private var mSendButton: Button? = null

    private var mDatabase: FirebaseDatabase? = null
    private var mMessagesReference: DatabaseReference? = null
    private var mChildEventListener: ChildEventListener? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var mStorage: FirebaseStorage? = null
    private var mFileReference: StorageReference? = null
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null

    private var mUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsername = ANONYMOUS
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        mMessagesReference = mDatabase!!.reference.child("messages")
        mFileReference = mStorage!!.reference.child("chat_photos")

        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar) as ProgressBar
        mMessageListView = findViewById(R.id.messageListView) as ListView
        mPhotoPickerButton = findViewById(R.id.photoPickerButton) as ImageButton
        mMessageEditText = findViewById(R.id.messageEditText) as EditText
        mSendButton = findViewById(R.id.sendButton) as Button

        // Initialize message ListView and its adapter
        val messages = ArrayList<Message>()
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, messages)
        mMessageListView!!.adapter = mMessageAdapter

        // Initialize progress bar
        mProgressBar!!.visibility = ProgressBar.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton!!.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER)
        }

        // Enable Send button when there's text to send
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mSendButton!!.isEnabled = charSequence.toString().length > 0
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        mMessageEditText!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))

        // Send button sends a message and clears the EditText
        mSendButton!!.setOnClickListener {
            val message = Message(mMessageEditText!!.text.toString(), mUsername, null)
            mMessagesReference!!.push().setValue(message)
            mMessageEditText!!.setText("")
        }

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                onSignedIn(user.displayName)
                Toast.makeText(this@MainActivity, "You're now signed in. Welcome to FriendlyChat.", Toast.LENGTH_SHORT).show()
            } else {
                onSighOut()
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(true)
                                .setProviders(AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER)
                                .build(),
                        RC_SIGN_IN)
            }
        }

        // Create Remote Config Setting to enable developer mode.
        // Fetching configs from the server is normally limited to 5 requests per hour.
        // Enabling developer mode allows many more requests to be made per hour, so developers
        // can test different config values during development.
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        mFirebaseRemoteConfig!!.setConfigSettings(configSettings)

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT)
        mFirebaseRemoteConfig!!.setDefaults(defaultConfigMap)
        fetchConfig()
    }

    private fun onSighOut() {
        mUsername = ANONYMOUS
        detachListener()
        mMessageAdapter!!.clear()
    }

    private fun onSignedIn(name: String?) {
        mUsername = name
        attachListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out_menu) {
            AuthUI.getInstance().signOut(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mAuth!!.addAuthStateListener(mAuthListener!!)
        attachListener()
    }

    override fun onPause() {
        super.onPause()
        mMessageAdapter!!.clear()
        mAuth!!.removeAuthStateListener(mAuthListener!!)
        detachListener()
    }

    private fun attachListener() {
        if (mChildEventListener == null) {
            mChildEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String) {
                    val fm = dataSnapshot.getValue<Message>(Message::class.java)
                    mMessageAdapter!!.add(fm)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String) {
                    val fm = dataSnapshot.getValue<Message>(Message::class.java)
                    val i = mMessageAdapter!!.getPosition(fm)
                    if (i > 0) {
                        mMessageAdapter!!.insert(fm, i)
                    } else {
                        mMessageAdapter!!.add(fm)
                    }

                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    val fm = dataSnapshot.getValue<Message>(Message::class.java)
                    mMessageAdapter!!.remove(fm)
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String) {}

                override fun onCancelled(databaseError: DatabaseError) {}
            }
            mMessagesReference!!.addChildEventListener(mChildEventListener)
        }
    }

    private fun detachListener() {
        if (mChildEventListener != null) {
            mMessagesReference!!.removeEventListener(mChildEventListener!!)
            mChildEventListener = null
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUrl = data.data
            val ref = mFileReference!!.child(selectedImageUrl!!.lastPathSegment)
            ref.putFile(selectedImageUrl).addOnSuccessListener(this) { taskSnapshot ->
                // When the image has successfully uploaded, we get its download URL
                val downloadUrl = taskSnapshot.downloadUrl

                // Set the download URL to the message box, so that the user can send it to the database
                val message = Message(null, mUsername, downloadUrl!!.toString())
                mMessagesReference!!.push().setValue(message)
            }
        }
    }

    fun fetchConfig() {
        var cacheExpiration: Long = 3600 // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig!!.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        mFirebaseRemoteConfig!!.fetch(cacheExpiration)
                .addOnSuccessListener {
                    // Make the fetched config available
                    // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                    mFirebaseRemoteConfig!!.activateFetched()

                    // Update the EditText length limit with
                    // the newly retrieved values from Remote Config.
                    applyRetrievedLengthLimit()
                }
                .addOnFailureListener { e ->
                    // An error occurred when fetching the config.
                    Log.w(TAG, "Error fetching config", e)

                    // Update the EditText length limit with
                    // the newly retrieved values from Remote Config.
                    applyRetrievedLengthLimit()
                }
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private fun applyRetrievedLengthLimit() {
        val friendly_msg_length = mFirebaseRemoteConfig!!.getLong(FRIENDLY_MSG_LENGTH_KEY)
        mMessageEditText!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(friendly_msg_length.toInt()))
        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length)
    }

    companion object {

        private val TAG = "MainActivity"

        val ANONYMOUS = "anonymous"
        val FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length"
        val DEFAULT_MSG_LENGTH_LIMIT = 1000

        private val RC_SIGN_IN = 1
        private val RC_PHOTO_PICKER = 2
    }
}
