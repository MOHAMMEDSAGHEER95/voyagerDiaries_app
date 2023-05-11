import android.content.Context
import com.example.voyagerdiaries.Review
import org.postgresql.util.PSQLException
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
                    editor.putString("userName", username)
                    editor.putString("lastName", lastName)
                    editor.apply()
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
        }
        return authenticationSuccess
    }


    fun postUserReviews(reviews: String){
        val thread = Thread {
            val voyagerdiariesPref = context.getSharedPreferences("voyagerdiariesPref", Context.MODE_PRIVATE)
            val userId = voyagerdiariesPref.getString("id", null);
            val query = "insert into reviews (review, user_id) values ('$reviews','$userId') returning id";
            try {
                val statement = connection?.createStatement();
                val resultSet = statement?.executeQuery(query);
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
        try {
            thread.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateProfile(firstName: String, lastName: String){
        val thread = Thread {
            val voyagerdiariesPref = context.getSharedPreferences("voyagerdiariesPref", Context.MODE_PRIVATE)
            val userId = voyagerdiariesPref.getString("id", null);
            val query = "update users set first_name='$firstName', last_name='$lastName' where id=$userId returning id";
            try {
                val statement = connection?.createStatement();
                val resultSet = statement?.executeQuery(query);
                if(resultSet?.next() == true){
                    val editor = voyagerdiariesPref.edit();
                    editor.putString("firstName", firstName);
                    editor.putString("lastName", lastName);
                    editor.apply()
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
        }
    }

    fun getAllReview(userId: String? = null, usersReview: Boolean = false): MutableList<Review>{
        val reviewList = mutableListOf<Review>();
        val thread = Thread {
            var query = "select a.review,b.username,a.id from reviews a join users b on a.user_id=b.id order by a.id desc;";
            if (userId!!.isNotEmpty()){
                query = "SELECT r.review, u.username, r.id, CASE WHEN l.user_id IS NULL THEN 0 ELSE 1 END AS liked FROM reviews r LEFT JOIN liked_reviews l ON l.review_id = r.id AND l.user_id = $userId JOIN users u ON r.user_id = u.id ORDER BY r.id DESC; "
                if(usersReview){
                    query = query.replace("ORDER BY", "WHERE r.user_id=$userId ORDER BY")
                }
            }
            println(query)
            try {
                val statement = connection?.createStatement();
                val resultSet = statement?.executeQuery(query);
                while (resultSet?.next() == true) {
                    reviewList.add(Review(resultSet.getString("review"), resultSet.getString("username"), resultSet.getInt("id"), resultSet.getInt("liked")))
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
        }
        return reviewList
    }

    fun likeReview(userId: String, reviewId: Int): Boolean{
        var likedReview = false;
        val thread = Thread {
            val statement = connection?.createStatement();
            try {
                val query =
                    "insert into liked_reviews (user_id,review_id) values ($userId,$reviewId) returning id"

                val resultSet = statement?.executeQuery(query);
                likedReview = true;
            } catch (e: PSQLException){
                if(e.message.toString().contains("duplicate key value violates unique constraint")){
                    val deleteQuery = "delete from liked_reviews where user_id=$userId and review_id=$reviewId returning id"
                    statement?.executeQuery(deleteQuery)
                }
            }

            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        thread.start()
        try {
            thread.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return likedReview
    }

}