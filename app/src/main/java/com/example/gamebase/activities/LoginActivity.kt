package com.example.gamebase.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.gamebase.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.util.concurrent.Executor


class LoginActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 120
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isFinger: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        isFinger = intent.getBooleanExtra("isFinger", false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val registerButton       = findViewById<Button>(R.id.loginButton)
        val goToRegisterButton   = findViewById<Button>(R.id.goToRegisterButton)
        val registerGoogleButton = findViewById<ImageButton>(R.id.LoginGoogleButton)
        val forgotPasswordButton = findViewById<Button>(R.id.forgotPassword)
        registerButton.setOnClickListener       { signIn() }
        goToRegisterButton.setOnClickListener   { goToRegister() }
        registerGoogleButton.setOnClickListener {

            if (isFinger) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                signInViaGoogle()
            }
        }
            forgotPasswordButton.setOnClickListener { forgotPassword() }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    signInViaGoogle()
                    Toast.makeText(applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Log in using your finger. You can turn it off in settings after You log in.")
            .setNegativeButtonText("Use account password")
            .build()
    }

    private fun signIn ()
    {
        val email = findViewById<TextInputLayout>(R.id.emailTextFieldL).editText?.text.toString()
        val password = findViewById<TextInputLayout>(R.id.passwordTextFieldL).editText?.text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d("Login", "signInWithEmail:success")
                    val intentGameBase = Intent(this, GameBaseActivity::class.java)
                    startActivity(intentGameBase)

                } else {

                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }
            }
    }

    private fun goToRegister()
    {
        val intentRegister = Intent(this, RegisterActivity::class.java)
        intentRegister.putExtra("isFinger", isFinger)
        startActivity(intentRegister)
    }

    private fun signInViaGoogle()
    {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception
            if (task.isSuccessful) {
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("Google Login", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w("Google Login", "Google sign in failed", e)
                }
            } else {
                Log.w("Google Login", exception)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Google Login Credential", "signInWithCredential:success")
                    val intentGameBase = Intent(this, GameBaseActivity::class.java)
                    startActivity(intentGameBase)
                    finish()
                } else {
                    Log.w("Google Login Credential", "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun forgotPassword()
    {
        val email = findViewById<TextInputLayout>(R.id.emailTextFieldL).editText?.text.toString()
        if (email.isEmailValid())
        {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("ForgotPasswordTag", "Email sent.")
                            Toast.makeText(this@LoginActivity, "Reset mail sent. Check your email.", Toast.LENGTH_LONG).show()
                        }
                    }
        } else {
            Toast.makeText(this@LoginActivity, "Wrong email format! Enter email in correct form above.", Toast.LENGTH_LONG).show()
        }
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

}