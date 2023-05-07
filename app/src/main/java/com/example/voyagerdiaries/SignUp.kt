package com.example.voyagerdiaries

import Database
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUp : AppCompatActivity() {
    private val user = "voyageradmin"
    private val pass = "voyageradmin"
    private var url = "jdbc:postgresql://10.0.2.2:5432/voyager_db"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);


        val button = findViewById<Button>(R.id.signup);
        val firstName = findViewById<EditText>(R.id.editTextFirstName);
        val lastName = findViewById<EditText>(R.id.editTextLastName);
        val userName = findViewById<EditText>(R.id.editTextUsername);
        val password = findViewById<EditText>(R.id.editTextPassword);
        button.setOnClickListener {
            val formValid = validateInput(firstName.text.toString(), lastName.text.toString(), userName.text.toString(), password.text.toString())
            if(formValid) {
                val db = Database(this)
                val userAdded = db.addNewUser(
                    firstName.text.toString().trim(), lastName.text.toString().trim(),
                    userName.text.toString().trim(), password.text.toString().trim()
                )
                if (userAdded == true) {
                    Toast.makeText(this, "User Created successfully.", Toast.LENGTH_SHORT).show()
                    val mainActivityIntent = Intent(this, MainActivity::class.java)
                    startActivity(mainActivityIntent)
                } else {
                    Toast.makeText(this, "Unable to create account..", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun validateInput(firstName: String, lastName: String, userName: String, password: String): Boolean{
        if(firstName.length <= 0){
            Toast.makeText(this, "First Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if(userName.length <= 0){
            Toast.makeText(this, "User Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if(password.length <= 0){
            Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}