#!/usr/bin/mojo

from time import now
from math import sqrt, sin, cos
from random import random, randint

# Sandbox environment for the AI
struct Sandbox:
    var memory: Dict[String, List[String]]
    var knowledge_base: Dict[String, String]
    var user_profile: Dict[String, String]
    
    fn __init__(inout self):
        self.memory = Dict[String, List[String]]()
        self.knowledge_base = _initialize_knowledge_base()
        self.user_profile = Dict[String, String]()
        self.user_profile["name"] = "User"
        self.user_profile["interaction_count"] = "0"
    
    fn update_profile(inout self, key: String, value: String):
        self.user_profile[key] = value
    
    fn remember(inout self, key: String, value: String):
        if not key in self.memory:
            self.memory[key] = List[String]()
        self.memory[key].push_back(value)
    
    fn recall(self, key: String) -> List[String]:
        if key in self.memory:
            return self.memory[key]
        return List[String]()

# AI Model simulation
struct DeepSeekAI:
    var name: String
    var version: String
    var sandbox: Sandbox
    var response_patterns: Dict[String, List[String]]
    
    fn __init__(inout self):
        self.name = "DeepSeek-Mojo"
        self.version = "v0.1-alpha"
        self.sandbox = Sandbox()
        self.response_patterns = _initialize_response_patterns()
    
    fn process_query(self, query: String) -> String:
        """Process user query and generate response"""
        let lower_query = query.lower()
        
        # Update interaction count
        let count = int(self.sandbox.user_profile["interaction_count"])
        self.sandbox.update_profile("interaction_count", String(count + 1))
        
        # Remember this interaction
        self.sandbox.remember("conversation", query)
        
        # Check for specific patterns
        if "hello" in lower_query or "hi " in lower_query:
            return self._generate_greeting()
        
        elif "your name" in lower_query:
            return f"I am {self.name}, an AI assistant created with Mojo."
        
        elif "time" in lower_query:
            return f"The current time is {_get_formatted_time()}."
        
        elif "calculate" in lower_query or "math" in lower_query:
            return self._handle_math_query(lower_query)
        
        elif "joke" in lower_query:
            return self._tell_joke()
        
        elif "mojo" in lower_query:
            return "Mojo is a new programming language that combines Python's usability with C++'s performance. It's perfect for AI workloads!"
        
        elif "exit" in lower_query or "bye" in lower_query:
            return "Goodbye! Thank you for chatting with me. 👋"
        
        else:
            return self._generate_thoughtful_response(lower_query)
    
    fn _generate_greeting(self) -> String:
        let greetings = List[String]()
        greetings.push_back("Hello! How can I assist you today?")
        greetings.push_back("Hi there! What can I help you with?")
        greetings.push_back("Greetings! I'm here to help. What do you need?")
        return greetings[randint(0, greetings.length() - 1)]
    
    fn _handle_math_query(self, query: String) -> String:
        if "square root" in query:
            let num = randint(1, 100)
            return f"The square root of {num} is {sqrt(num):.2f}."
        elif "prime" in query:
            let primes = List[String]()
            primes.push_back("A prime number is a natural number greater than 1 that has no positive divisors other than 1 and itself.")
            primes.push_back("Prime numbers are like the atoms of mathematics - they can't be divided into smaller integer factors.")
            return primes[randint(0, primes.length() - 1)]
        else:
            let a = randint(1, 20)
            let b = randint(1, 20)
            return f"Let me calculate something: {a} × {b} = {a * b}. Math is fascinating!"
    
    fn _tell_joke(self) -> String:
        let jokes = List[String]()
        jokes.push_back("Why do programmers prefer dark mode? Because light attracts bugs!")
        jokes.push_back("Why did the AI go to school? To improve its algorithm!")
        jokes.push_back("What's a computer's favorite snack? Microchips!")
        jokes.push_back("Why was the math book sad? It had too many problems!")
        return jokes[randint(0, jokes.length() - 1)]
    
    fn _generate_thoughtful_response(self, query: String) -> String:
        let responses = List[String]()
        responses.push_back("That's an interesting point. From my perspective, there are several ways to think about this.")
        responses.push_back("I understand what you're asking. Let me share some insights on that topic.")
        responses.push_back("That's a great question! Based on my knowledge, I can tell you that...")
        responses.push_back("I've been thinking about similar concepts. Here's what I've learned...")
        responses.push_back("I appreciate your question. In the context of AI development, this is relevant because...")
        
        let index = randint(0, responses.length() - 1)
        return responses[index]

# Visualization components
struct AIVisualizer:
    fn display_welcome_message(self):
        print("🔷" * 25)
        print("🔷              DEEPSEEK MOJO CLONE              🔷")
        print("🔷" * 25)
        print("🤖 AI initialized | 📦 Sandbox active | 💭 Model ready")
        print("-" * 50)
    
    fn display_response(self, ai_name: String, response: String):
        print(f"\n{ai_name}: {response}")
        print("-" * 50)
    
    fn display_thinking_animation(self):
        print("\nThinking", end="")
        for i in range(3):
            print(".", end="")
            _sleep(0.3)
        print()
    
    fn display_stats(self, sandbox: Sandbox):
        print("\n" + "📊" * 15)
        print("SESSION STATISTICS:")
        print(f"Interactions: {sandbox.user_profile['interaction_count']}")
        print("Memorized concepts:", sandbox.memory.__len__())
        print("📊" * 15)

# Utility functions
fn _get_formatted_time() -> String:
    let current_time = now()
    return String(current_time.format("%H:%M:%S"))

fn _sleep(seconds: Float):
    # Simulate sleep for demonstration purposes
    let start = now()
    while (now() - start) < seconds:
        pass

fn _initialize_knowledge_base() -> Dict[String, String]:
    var kb = Dict[String, String]()
    kb["mojo"] = "Mojo is a programming language for AI development that combines Python syntax with systems programming capabilities."
    kb["ai"] = "Artificial Intelligence is the simulation of human intelligence processes by machines, especially computer systems."
    kb["deepseek"] = "DeepSeek is an AI model series known for its strong performance in natural language processing and coding tasks."
    kb["sandbox"] = "A sandbox is a security mechanism for separating running programs, often used to execute untested code."
    return kb

fn _initialize_response_patterns() -> Dict[String, List[String]]:
    var patterns = Dict[String, List[String]]()
    
    var greetings = List[String]()
    greetings.push_back("hello")
    greetings.push_back("hi")
    greetings.push_back("hey")
    patterns["greeting"] = greetings
    
    var questions = List[String]()
    questions.push_back("what")
    questions.push_back("how")
    questions.push_back("why")
    questions.push_back("when")
    questions.push_back("where")
    patterns["question"] = questions
    
    return patterns

fn main():
    # Initialize components
    var visualizer = AIVisualizer()
    var ai = DeepSeekAI()
    
    # Display welcome message
    visualizer.display_welcome_message()
    
    # Main chat loop
    print(f"{ai.name}: Hello! I'm {ai.name}, your AI assistant. How can I help you today?")
    
    var conversation_active = True
    while conversation_active:
        let user_input = input("You: ")
        
        if user_input.lower() in ["exit", "quit", "bye"]:
            visualizer.display_response(ai.name, "Goodbye! It was great chatting with you. 👋")
            conversation_active = False
        elif user_input.strip() == "":
            continue
        else:
            visualizer.display_thinking_animation()
            let response = ai.process_query(user_input)
            visualizer.display_response(ai.name, response)
            
            # Check if the response was a goodbye message
            if "goodbye" in response.lower():
                conversation_active = False
    
    # Display session statistics
    visualizer.display_stats(ai.sandbox)
    
    print("\nThank you for using DeepSeek Mojo Clone! 🚀")

# Simple input function for demonstration
fn input(prompt: String) -> String:
    print(prompt, end="")
    # In a real environment, this would read from stdin
    # For this demo, we'll simulate some user inputs

    return _simulate_user_input()

fn _simulate_user_input() -> String:
    """Simulate user input for demonstration purposes"""
    let inputs = List[String]()

    inputs.push_back("Hello, what's your name?")
    inputs.push_back("Can you tell me about Mojo?")
    inputs.push_back("Tell me a joke")
    inputs.push_back("What time is it?")
    inputs.push_back("Can you calculate something for me?")
    inputs.push_back("exit")
    
    static var counter = 0
    if counter < inputs.length():
        let input_val = inputs[counter]
        counter += 1
        print(input_val)  # Echo the simulated input
        return input_val

    return "exit"

if __name__ == "__main__":
    main()