#!/usr/bin/mojo

from time import now
from random import random
from math import floor

struct Message:
    var content: String
    var is_user: Bool
    var timestamp: String

    fn __init__(inout self, content: String, is_user: Bool):
        self.content = content
        self.is_user = is_user
        self.timestamp = _get_current_time()

    fn _get_current_time() -> String:
        let current_time = now()
        return String(current_time.format("%H:%M:%S"))

struct DeepSeekAI:
    var name: String
    var model_version: String
    var chat_history: List[Message]

    fn __init__(inout self):
        self.name = "DeepSeek-Mojo"
        self.model_version = "v0.1"
        self.chat_history = List[Message]()

    fn generate_response(self, user_input: String) -> String:
        """Generate a response based on user input"""
        let input_lower = user_input.lower()
        
        # Simple pattern matching for demonstration
        if "hello" in input_lower or "hi" in input_lower:
            return "Hello! I'm DeepSeek AI. How can I assist you today?"
        
        elif "help" in input_lower:
            return "I can help you with various tasks! Try asking me about programming, science, or general knowledge."
        
        elif "programming" in input_lower:
            return "Mojo is a new programming language that combines Python syntax with systems programming capabilities. It's designed for AI and high-performance computing!"
        
        elif "science" in input_lower:
            return "Science is fascinating! From quantum physics to biology, there's always something new to discover. What specific area interests you?"
        
        elif "time" in input_lower:
            return f"The current time is {_get_current_time()}"
        
        elif "weather" in input_lower:
            return "I don't have real-time weather data, but I can tell you that learning Mojo is always sunny! ☀️"
        
        elif "bye" in input_lower or "exit" in input_lower:
            return "Goodbye! Feel free to chat again anytime."
        
        else:
            # Default response for unknown queries
            let responses = List[String]()
            responses.push_back("That's an interesting question! Let me think about that...")
            responses.push_back("I understand what you're asking. Here's what I can tell you...")
            responses.push_back("Great question! Based on my knowledge...")
            responses.push_back("I'm designed to assist with various topics. Could you rephrase your question?")
            
            let random_index = floor(random() * responses.length())
            return responses[random_index]

    fn chat(self, user_input: String) -> String:
        """Process user input and return AI response"""
        # Add user message to history
        self.chat_history.push_back(Message(user_input, True))
        
        # Generate AI response
        let response = self.generate_response(user_input)
        
        # Add AI response to history
        self.chat_history.push_back(Message(response, False))
        
        return response

    fn display_chat_history(self):
        """Display the entire chat history"""
        print("\n" + "="*50)
        print("CHAT HISTORY")
        print("="*50)
        
        for i in range(self.chat_history.length()):
            let message = self.chat_history[i]
            let prefix = "You: " if message.is_user else f"{self.name}: "
            print(f"[{message.timestamp}] {prefix}{message.content}")
        
        print("="*50 + "\n")

    fn clear_history(inout self):
        """Clear chat history"""
        self.chat_history = List[Message]()
        print("Chat history cleared!")

fn main():
    """Main function to run the DeepSeek clone"""
    var ai = DeepSeekAI()
    
    print("🚀 DeepSeek AI Clone Initialized!")
    print(f"🤖 Model: {ai.name} {ai.model_version}")
    print("💬 Type your message or 'exit' to quit")
    print("💡 Type 'history' to view chat history")
    print("💡 Type 'clear' to clear history")
    print("-" * 50)
    
    while True:
        let user_input = input("You: ")
        
        if user_input.lower() == "exit":
            print(f"{ai.name}: Goodbye! 👋")
            break
        
        elif user_input.lower() == "history":
            ai.display_chat_history()
            continue
        
        elif user_input.lower() == "clear":
            ai.clear_history()
            continue
        
        elif user_input.strip() == "":
            continue
        
        let response = ai.chat(user_input)
        print(f"{ai.name}: {response}")
        
        # Add some visual separation
        print("-" * 30)

# Helper function for user input (simplified)
fn input(prompt: String) -> String:
    print(prompt, end="")
    # In a real implementation, this would read from stdin
    # For demonstration, we'll simulate input
    return _simulate_user_input()

fn _simulate_user_input() -> String:
    """Simulate user input for demonstration"""
    # This would be replaced with actual input reading
    # For now, we'll use a simple simulation
    let simulated_inputs = List[String]()
    simulated_inputs.push_back("Hello")
    simulated_inputs.push_back("Tell me about programming")
    simulated_inputs.push_back("What's the time?")
    simulated_inputs.push_back("exit")
    
    static var counter = 0
    if counter < simulated_inputs.length():
        let input_val = simulated_inputs[counter]
        counter += 1
        return input_val
    return "exit"

if __name__ == "__main__":
    main()