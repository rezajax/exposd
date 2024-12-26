package com.example

import com.example.db.Tasks
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

// Singleton for database connection
object DatabaseFactory {
    fun init() {
        try {
//            Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver", user = "root", password = "")
            Database.connect("jdbc:h2:file:./testdb", driver = "org.h2.Driver", user = "root", password = "")

            transaction {
                // Print SQL to stdout
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Tasks)

                Tasks.insert {
                    it[title] = "Learn Exposed"
                    it[description] = "Go through the Get started with Exposed tutorial"
                }

                Tasks.insert {
                    it[title] = "Read The Hobbit"
                    it[description] = "Read the first two chapters of The Hobbit"
                    it[isCompleted] = true
                }
            }
        } catch (e: Exception) {
            println("Error: $e")
        }
    }
}

fun Application.configureRouting() {
    // Initialize the database connection only once
    DatabaseFactory.init()

    routing {
        get("/") {
            val tasksHtml = fetchTasksHtml()
            call.respondText(contentType = ContentType.Text.Html) {
                """
                <html>
                    <body>
                        <h1>Task List</h1>
                        <table border="1">
                            <tr>
                                <th>ID</th>
                                <th>Title</th>
                                <th>Description</th>
                                <th>Is Completed</th>
                            </tr>
                            $tasksHtml
                        </table>
                    </body>
                </html>
                """.trimIndent()
            }
        }
    }
}

fun fetchTasksHtml(): String {
    // No need to reconnect to the database here
    return transaction {
        Tasks.selectAll().joinToString(separator = "") { task ->
            """
            <tr>
                <td>${task[Tasks.id]}</td>
                <td>${task[Tasks.title]}</td>
                <td>${task[Tasks.description]}</td>
                <td>${if (task[Tasks.isCompleted]) "Yes" else "No"}</td>
            </tr>
            """.trimIndent()
        }
    }
}
