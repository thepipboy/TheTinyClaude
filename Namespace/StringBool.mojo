#!/usr/bin/mojo

from math import sqrt, sin, cos, exp, pi
from random import random, gauss
from time import now
from tensor import Tensor
from complex import Complex

# StringBool type definition - a quantum string with boolean properties
struct StringBool:
    var quantum_state: Complex  # Quantum amplitude
    var vibrational_mode: Int   // Current vibration mode
    var spin: Int              // Spin property
    var entangled: Bool        // Entanglement state
    var dimensions: Int        // Dimensionality (up to 12)
    
    fn __init__(inout self, dimensions: Int = 12) -> None:
        self.quantum_state = Complex(1.0, 0.0)  # Ground state
        self.vibrational_mode = 0
        self.spin = 1
        self.entangled = False
        self.dimensions = dimensions
    
    fn excite(inout self, energy: Float) -> None:
        """Excite the string to a higher vibrational mode"""
        let probability = exp(-energy) * (1.0 - self.quantum_state.norm())
        if random() < probability:
            self.vibrational_mode += 1
            self.quantum_state = Complex(random() - 0.5, random() - 0.5).normalized()
    
    fn measure(self) -> Bool:
        """Measure the boolean state of the string"""
        let prob_true = self.quantum_state.real * self.quantum_state.real
        return random() < prob_true
    
    fn entangle(inout self, other: StringBool) -> None:
        """Entangle two string bools"""
        if not self.entangled and not other.entangled:
            self.entangled = True
            other.entangled = True
            # Create correlated quantum state
            let new_state = Complex(1/sqrt(2), 0)
            self.quantum_state = new_state
            other.quantum_state = new_state
    
    fn __str__(self) -> String:
        return f"StringBool(mode={self.vibrational_mode}, spin={self.spin}, entangled={self.entangled})"

# 12-Dimensional Boson Field Simulation
struct BosonField12D:
    var dimensions: Int
    var field_strength: Tensor[Float]
    var strings: List[StringBool]
    var coupling_constant: Float
    
    fn __init__(inout self, dimensions: Int = 12) -> None:
        self.dimensions = dimensions
        self.field_strength = Tensor[Float](shape=(dimensions, dimensions))
        self.strings = List[StringBool]()
        self.coupling_constant = 0.1
        
        # Initialize field with random values
        for i in range(dimensions):
            for j in range(dimensions):
                self.field_strength[i, j] = random()
    
    fn add_string(inout self, string: StringBool) -> None:
        """Add a string to the field"""
        self.strings.push_back(string)
    
    fn propagate_strings(inout self, steps: Int) -> None:
        """Propagate strings through the field"""
        for step in range(steps):
            for i in range(self.strings.length()):
                let energy = self._calculate_energy(i)
                self.strings[i].excite(energy)
                
                # Occasionally entangle strings
                if random() < 0.05 and self.strings.length() > 1:
                    let j = randint(0, self.strings.length() - 1)
                    if i != j:
                        self.strings[i].entangle(self.strings[j])
    
    fn _calculate_energy(self, string_index: Int) -> Float:
        """Calculate energy for a string based on field interactions"""
        var energy = 0.0
        let string = self.strings[string_index]
        
        for i in range(self.dimensions):
            for j in range(self.dimensions):
                energy += self.field_strength[i, j] * (string.vibrational_mode + 1) * self.coupling_constant
        
        return energy / (self.dimensions * self.dimensions)
    
    fn measure_all(self) -> List[Bool]:
        """Measure all strings in the field"""
        var results = List[Bool]()
        for string in self.strings:
            results.push_back(string.measure())
        return results
    
    fn get_entanglement_graph(self) -> Dict[Int, List[Int]]:
        """Get graph of entangled strings"""
        var graph = Dict[Int, List[Int]]()
        
        for i in range(self.strings.length()):
            if self.strings[i].entangled:
                graph[i] = List[Int]()
                for j in range(self.strings.length()):
                    if i != j and self.strings[j].entangled:
                        graph[i].push_back(j)
        
        return graph

# Sandbox environment for quantum string experiments
struct QuantumSandbox:
    var boson_field: BosonField12D
    var history: List[List[Bool]]
    var step_count: Int
    
    fn __init__(inout self) -> None:
        self.boson_field = BosonField12D(12)
        self.history = List[List[Bool]]()
        self.step_count = 0
        
        # Initialize with some strings
        for i in range(5):
            self.boson_field.add_string(StringBool(12))
    
    fn run_experiment(inout self, steps: Int) -> None:
        """Run a quantum experiment"""
        print("Starting quantum experiment...")
        
        for step in range(steps):
            self.boson_field.propagate_strings(1)
            let measurements = self.boson_field.measure_all()
            self.history.push_back(measurements)
            self.step_count += 1
            
            if step % 10 == 0:
                print(f"Step {step}: {self._calculate_entropy()} bits of entropy")
    
    fn _calculate_entropy(self) -> Float:
        """Calculate information entropy of the system"""
        if self.history.length() == 0:
            return 0.0
        
        let current = self.history[self.history.length() - 1]
        var counts = Dict[Bool, Int]()
        counts[True] = 0
        counts[False] = 0
        
        for value in current:
            counts[value] += 1
        
        let total = current.length()
        var entropy = 0.0
        
        for count in counts.values():
            if count > 0:
                let probability = count / total
                entropy -= probability * log2(probability)
        
        return entropy
    
    fn display_quantum_state(self) -> None:
        """Display the current quantum state"""
        print("\n" + "="*60)
        print("QUANTUM SANDBOX STATE")
        print("="*60)
        
        print(f"Dimensions: {self.boson_field.dimensions}")
        print(f"Strings: {self.boson_field.strings.length()}")
        print(f"Steps: {self.step_count}")
        print(f"Entropy: {self._calculate_entropy():.3f} bits")
        
        # Display entanglement graph
        let graph = self.boson_field.get_entanglement_graph()
        if graph.__len__() > 0:
            print("\nEntanglement Graph:")
            for key in graph.keys():
                print(f"  String {key} entangled with: {graph[key]}")
        
        # Display recent measurements
        if self.history.length() > 0:
            print(f"\nRecent measurements (last 5 steps):")
            let start = max(0, self.history.length() - 5)
            for i in range(start, self.history.length()):
                print(f"  Step {i}: {self.history[i]}")
        
        print("="*60)

# Visualization utilities for the quantum system
struct QuantumVisualizer:
    fn display_welcome(self) -> None:
        print("🔬" * 25)
        print("🔬        12-D BOSON STRING SANDBOX         🔬")
        print("🔬" * 25)
        print("🌌 Dimensions: 12")
        print("⚛️  StringBool particles: 5")
        print("🌀 Quantum entanglement enabled")
        print("-" * 50)
    
    fn display_string_properties(self, strings: List[StringBool]) -> None:
        print("\nString Properties:")
        for i in range(strings.length()):
            let s = strings[i]
            print(f"  {i}: mode={s.vibrational_mode}, spin={s.spin}, entangled={s.entangled}")
    
    fn display_field_energy(self, field: BosonField12D) -> None:
        var total_energy = 0.0
        for i in range(field.dimensions):
            for j in range(field.dimensions):
                total_energy += field.field_strength[i, j]
        
        print(f"\nField Energy: {total_energy:.3f}")
        print(f"Coupling Constant: {field.coupling_constant}")

# Main simulation
fn main():
    var visualizer = QuantumVisualizer()
    visualizer.display_welcome()
    
    var sandbox = QuantumSandbox()
    
    # Run the experiment
    sandbox.run_experiment(50)
    
    # Display results
    sandbox.display_quantum_state()
    visualizer.display_string_properties(sandbox.boson_field.strings)
    visualizer.display_field_energy(sandbox.boson_field)
    
    print("\nExperiment completed! 🎉")

# Utility functions
fn randint(low: Int, high: Int) -> Int:
    return low + Int(random() * (high - low + 1))

fn log2(x: Float) -> Float:
    return log(x) / log(2.0)

# Mock Tensor implementation for demonstration
struct Tensor[T: AnyType]:
    var shape: List[Int]
    var data: List[T]
    
    fn __init__(inout self, shape: List[Int]) -> None:
        self.shape = shape
        var size = 1
        for dim in shape:
            size *= dim
        self.data = List[T](size)
    
    fn __getitem__(self, indices: List[Int]) -> T:
        var index = 0
        var stride = 1
        for i in range(len(indices)):
            index += indices[i] * stride
            stride *= self.shape[i]
        return self.data[index]
    
    fn __setitem__(inout self, indices: List[Int], value: T) -> None:
        var index = 0
        var stride = 1
        for i in range(len(indices)):
            index += indices[i] * stride
            stride *= self.shape[i]
        self.data[index] = value

# Mock Complex number implementation
struct Complex:
    var real: Float
    var imag: Float
    
    fn __init__(inout self, real: Float, imag: Float) -> None:
        self.real = real
        self.imag = imag
    
    fn norm(self) -> Float:
        return sqrt(self.real * self.real + self.imag * self.imag)
    
    fn normalized(self) -> Complex:
        let n = self.norm()
        if n == 0.0:
            return Complex(0.0, 0.0)
        return Complex(self.real / n, self.imag / n)
if __name__ == "__main__":
    main()