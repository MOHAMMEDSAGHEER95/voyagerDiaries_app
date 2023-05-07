import android.content.Context
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

class Database (context: Context){
    private var connection: Connection? = null
    private val user = "voyageradmin"
    private val pass = "voyageradmin"
    private var url = "jdbc:postgresql://10.0.2.2:5432/voyager_db"
    private var status = false
    private var context: Context = context

    init {
        connect()
        println("connection status:$status")
    }

    private fun connect() {
        val thread = Thread {
            try {
                Class.forName("org.postgresql.Driver")
                connection = DriverManager.getConnection(url, user, pass)
                status = true
            } catch (e: Exception) {
                status = false
                print(e.message)
                e.printStackTrace()
            }
        }
        thread.start()
        try {
            thread.join()
        } catch (e: Exception) {
            e.printStackTrace()
            status = false
        }
    }

    fun addNewUser(firstName: String, lastName: String, userName: String, password: String): Boolean {
        var userAdded = false;
        val thread = Thread {
            val encryptedPassword = MessageDigest.getInstance("SHA-1").digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
            val query = "INSERT INTO users (first_name, last_name, username, password) values ('$firstName', '$lastName', '$userName', '$encryptedPassword') returning id"
            try {
                val statement = connection?.createStatement();
                val resultSet = statement?.executeQuery(query);
                userAdded = true;
            } catch (e: Exception) {
                userAdded = false;
                e.printStackTrace()
            }
        }
        thread.start()
        try {
            thread.join()
        } catch (e: Exception) {
            e.printStackTrace()
            status = false
        }
        return userAdded
    }


    fun authenticateUser(userName: String, password: String): Boolean{
        var authenticationSuccess = false;
        val thread = Thread {
            val encryptedPassword = MessageDigest.getInstance("SHA-1").digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
            val query = "select * from  users where username='$userName' and password='$encryptedPassword'"

            try {
                val statement = connection?.createStatement();
                val resultSet = statement?.executeQuery(query);
                if(resultSet?.next() == true){
                    authenticationSuccess = true
                    val id = resultSet.getInt("id")
                    val firstName = resultSet.getString("first_name")
                    val lastName = resultSet.getString("last_name")
                    val username = resultSet.getString("username")
                    val voyagerdiariesPref = context.getSharedPreferences("voyagerdiariesPref", Context.MODE_PRIVATE)
                    val editor = voyagerdiariesPref.edit()
                    editor.putString("id", id.toString())
                    editor.putString("firstName", firstName)
                    editor.putString("username", username)
                    editor.putString("lastName", lastName)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        try {
            thread.join()
        } catch (e: Exception) {
            e.printStackTrace()
            status = false
        }
        return authenticationSuccess
    }

}