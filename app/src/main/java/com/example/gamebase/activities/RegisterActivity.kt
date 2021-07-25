package com.example.gamebase.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
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

class RegisterActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        isFinger = intent.getBooleanExtra("isFinger", false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val registerButton       = findViewById<Button>(R.id.registerButton)
        val goToLoginButton      = findViewById<Button>(R.id.goToLoginButton)
        val registerGoogleButton = findViewById<AppCompatImageButton>(R.id.registerGoogleButton)
        val agreeCheckBox        = findViewById<CheckBox>(R.id.agreeCheckbox)
        registerButton.setOnClickListener      { signUp() }
        goToLoginButton.setOnClickListener     { goToLogin() }
        registerGoogleButton.setOnClickListener{

            if (isFinger) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                signInViaGoogle()
            }
        }
        agreeCheckBox.setOnClickListener       { agreeTaC() }

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

    private fun signUp ()
    {
        val email = findViewById<TextInputLayout>(R.id.emailTextFieldR).editText?.text.toString()
        val password = findViewById<TextInputLayout>(R.id.passwordTextFieldR).editText?.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d("Register", "createUserWithEmail:success")
                    val intentGameBase = Intent(this, GameBaseActivity::class.java)
                    startActivity(intentGameBase)

                } else {

                    Log.w("Register", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }
            }
    }

    private fun goToLogin()
    {
        val intentRegister = Intent(this, LoginActivity::class.java)
        intentRegister.putExtra("isFinger", isFinger)
        startActivity(intentRegister)
    }

    private fun agreeTaC()
    {
        val registerButton = findViewById<Button>(R.id.registerButton)
        val checkbox = findViewById<CheckBox>(R.id.agreeCheckbox)
        registerButton.isEnabled = checkbox.isChecked
    }

    private fun signInViaGoogle()
    {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RegisterActivity.RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RegisterActivity.RC_SIGN_IN) {
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

}