package com.example.day

import com.example.day.data.AgentRepository
import org.junit.Assert.*
import org.junit.Test

class AgentToolParsingTest {

    @Test
    fun testParseGeminiResponseWithToolBlock() {
        val raw = """
I've scheduled your DSA Study session and added your assignment to your todos!

```tools
[
  {
    "tool": "add_task",
    "args": {
      "name": "DSA Study",
      "start_time": "14:00",
      "end_time": "15:30"
    }
  },
  {
    "tool": "add_todo",
    "args": {
      "title": "Submit assignment",
      "priority": 1,
      "due_date": "2026-10-22"
    }
  }
]
```
        """.trimIndent()

        val parsed = AgentRepository.parseGeminiResponse(raw)

        assertEquals("I've scheduled your DSA Study session and added your assignment to your todos!", parsed.conversationalText)
        assertEquals(2, parsed.toolCalls.size)

        val taskTool = parsed.toolCalls[0]
        assertEquals("add_task", taskTool.tool)
        assertEquals("DSA Study", taskTool.args["name"])
        assertEquals("14:00", taskTool.args["start_time"])
        assertEquals("15:30", taskTool.args["end_time"])

        val todoTool = parsed.toolCalls[1]
        assertEquals("add_todo", todoTool.tool)
        assertEquals("Submit assignment", todoTool.args["title"])
        assertEquals(1, (todoTool.args["priority"] as Number).toInt())
        assertEquals("2026-10-22", todoTool.args["due_date"])
    }

    @Test
    fun testParseGeminiResponseWithoutToolBlock() {
        val raw = "You have 3 tasks scheduled for today, and 2 pending todos."
        val parsed = AgentRepository.parseGeminiResponse(raw)

        assertEquals("You have 3 tasks scheduled for today, and 2 pending todos.", parsed.conversationalText)
        assertTrue(parsed.toolCalls.isEmpty())
    }

    @Test
    fun testParseSingleToolObject() {
        val jsonStr = """
        {
          "tool": "complete_task",
          "args": {
            "name": "Morning Run"
          }
        }
        """.trimIndent()

        val tools = AgentRepository.parseToolsJson(jsonStr)
        assertEquals(1, tools.size)
        assertEquals("complete_task", tools[0].tool)
        assertEquals("Morning Run", tools[0].args["name"])
    }

    @Test
    fun testParseMalformedToolsJson() {
        val raw = """
Here is your answer!
```tools
invalid json [[[
```
        """.trimIndent()

        val parsed = AgentRepository.parseGeminiResponse(raw)
        assertEquals("Here is your answer!", parsed.conversationalText)
        assertTrue(parsed.toolCalls.isEmpty())
    }
}
