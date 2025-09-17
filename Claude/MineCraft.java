import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class MinecraftClone {
    // Window dimensions
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private long window;
    
    // Shader program
    private int shaderProgram;
    
    // World parameters
    private static final int CHUNK_SIZE = 16;
    private static final int CHUNK_HEIGHT = 64;
    private static final int RENDER_DISTANCE = 8; // chunks in each direction
    private Chunk[][] chunks;
    
    // Player/camera
    private Camera camera;
    private InputHandler inputHandler;
    
    // Textures
    private int textureAtlas;
    
    // Block types
    public enum BlockType {
        AIR(0, false, false),
        GRASS(1, true, true),
        DIRT(2, true, true),
        STONE(3, true, true),
        WATER(4, false, true),
        WOOD(5, true, true),
        LEAVES(6, true, true);
        
        public final int id;
        public final boolean isSolid;
        public final boolean isVisible;
        
        BlockType(int id, boolean isSolid, boolean isVisible) {
            this.id = id;
            this.isSolid = isSolid;
            this.isVisible = isVisible;
        }
        
        public static BlockType fromId(int id) {
            for (BlockType type : values()) {
                if (type.id == id) return type;
            }
            return AIR;
        }
    }
    
    // Camera class
    class Camera {
        public float x, y, z;
        public float rx, ry; // rotation x (pitch), rotation y (yaw)
        public float speed = 5.0f;
        public float mouseSensitivity = 0.15f;
        
        public Camera(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void update(float deltaTime) {
            // Calculate movement direction based on rotation
            float dx = (float) (Math.sin(Math.toRadians(ry)) * speed * deltaTime);
            float dz = (float) (Math.cos(Math.toRadians(ry)) * speed * deltaTime);
            
            // Handle keyboard input
            if (inputHandler.isKeyPressed(GLFW_KEY_W)) {
                x += dx;
                z += dz;
            }
            if (inputHandler.isKeyPressed(GLFW_KEY_S)) {
                x -= dx;
                z -= dz;
            }
            if (inputHandler.isKeyPressed(GLFW_KEY_A)) {
                x += dz;
                z -= dx;
            }
            if (inputHandler.isKeyPressed(GLFW_KEY_D)) {
                x -= dz;
                z += dx;
            }
            if (inputHandler.isKeyPressed(GLFW_KEY_SPACE)) {
                y += speed * deltaTime;
            }
            if (inputHandler.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
                y -= speed * deltaTime;
            }
            
            // Handle mouse input
            ry += inputHandler.getDeltaX() * mouseSensitivity;
            rx -= inputHandler.getDeltaY() * mouseSensitivity;
            
            // Clamp pitch to avoid flipping
            rx = Math.max(-90, Math.min(90, rx));
            
            // Reset mouse deltas
            inputHandler.resetDeltas();
        }
        
        public Matrix4f getViewMatrix() {
            Matrix4f matrix = new Matrix4f();
            matrix.rotate((float) Math.toRadians(rx), 1, 0, 0); // Pitch
            matrix.rotate((float) Math.toRadians(ry), 0, 1, 0); // Yaw
            matrix.translate(-x, -y, -z);
            return matrix;
        }
    }
    
    // Input handler class
    class InputHandler {
        private boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
        private double lastMouseX, lastMouseY;
        private double deltaX, deltaY;
        private boolean firstMouse = true;
        
        public void keyCallback(long window, int key, int scancode, int action, int mods) {
            if (key >= 0 && key < keys.length) {
                keys[key] = action != GLFW_RELEASE;
            }
        }
        
        public void cursorPosCallback(long window, double xpos, double ypos) {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            
            deltaX = xpos - lastMouseX;
            deltaY = ypos - lastMouseY;
            
            lastMouseX = xpos;
            lastMouseY = ypos;
        }
        
        public boolean isKeyPressed(int keyCode) {
            return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
        }
        
        public double getDeltaX() {
            return deltaX;
        }
        
        public double getDeltaY() {
            return deltaY;
        }
        
        public void resetDeltas() {
            deltaX = 0;
            deltaY = 0;
        }
    }
    
    // Chunk class
    class Chunk {
        public int x, z;
        public BlockType[][][] blocks;
        public int vao, vbo;
        public int vertexCount;
        
        public Chunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.blocks = new BlockType[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
            generateTerrain();
            createMesh();
        }
        
        private void generateTerrain() {
            Random random = new Random(x * 49632 + z * 325176);
            SimplexNoise noise = new SimplexNoise(random.nextInt());
            
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                    int worldX = x * CHUNK_SIZE + localX;
                    int worldZ = z * CHUNK_SIZE + localZ;
                    
                    // Generate height using noise
                    double height = noise.eval(worldX * 0.01, worldZ * 0.01) * 20 + 32;
                    
                    // Fill blocks
                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        if (y > height) {
                            if (y < 32) {
                                blocks[localX][y][localZ] = BlockType.WATER;
                            } else {
                                blocks[localX][y][localZ] = BlockType.AIR;
                            }
                        } else if (y == (int) height) {
                            blocks[localX][y][localZ] = BlockType.GRASS;
                        } else if (y > height - 4) {
                            blocks[localX][y][localZ] = BlockType.DIRT;
                        } else {
                            blocks[localX][y][localZ] = BlockType.STONE;
                        }
                    }
                    
                    // Generate trees
                    if (random.nextDouble() < 0.02 && height > 40) {
                        generateTree(localX, (int) height, localZ, random);
                    }
                }
            }
        }
        
        private void generateTree(int x, int y, int z, Random random) {
            int height = 4 + random.nextInt(3);
            
            // Generate trunk
            for (int i = 1; i <= height; i++) {
                if (y + i < CHUNK_HEIGHT) {
                    blocks[x][y + i][z] = BlockType.WOOD;
                }
            }
            
            // Generate leaves
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    for (int k = -2; k <= 2; k++) {
                        if (x + i >= 0 && x + i < CHUNK_SIZE && 
                            z + j >= 0 && z + j < CHUNK_SIZE &&
                            y + height + k < CHUNK_HEIGHT) {
                            
                            // Skip corners for a more natural shape
                            if (Math.abs(i) == 2 && Math.abs(j) == 2 && Math.abs(k) == 2) continue;
                            
                            blocks[x + i][y + height + k][z + j] = BlockType.LEAVES;
                        }
                    }
                }
            }
        }
        
        private void createMesh() {
            List<Float> vertices = new ArrayList<>();
            
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        BlockType block = blocks[x][y][z];
                        if (block.isVisible && block != BlockType.AIR) {
                            addBlockVertices(vertices, x, y, z, block);
                        }
                    }
                }
            }
            
            vertexCount = vertices.size() / 8; // 8 floats per vertex (position + texture + normal)
            
            // Convert list to float array
            float[] vertexData = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                vertexData[i] = vertices.get(i);
            }
            
            // Create VAO and VBO
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
            
            // Position attribute
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            
            // Texture coordinate attribute
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            
            // Normal attribute
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);
            glEnableVertexAttribArray(2);
            
            glBindVertexArray(0);
        }
        
        private void addBlockVertices(List<Float> vertices, int x, int y, int z, BlockType type) {
            int worldX = this.x * CHUNK_SIZE + x;
            int worldZ = this.z * CHUNK_SIZE + z;
            
            // Check which faces are visible (neighbors are transparent)
            boolean[] visibleFaces = new boolean[6];
            
            // Check negative X
            if (x == 0) {
                // Check neighboring chunk
                if (worldX - 1 >= 0) {
                    Chunk neighbor = chunks[x > 0 ? this.x - 1 : this.x][z];
                    visibleFaces[0] = neighbor == null || !neighbor.blocks[CHUNK_SIZE - 1][y][z].isSolid;
                } else {
                    visibleFaces[0] = true;
                }
            } else {
                visibleFaces[0] = !blocks[x - 1][y][z].isSolid;
            }
            
            // Check positive X
            if (x == CHUNK_SIZE - 1) {
                // Check neighboring chunk
                Chunk neighbor = chunks[x < CHUNK_SIZE - 1 ? this.x + 1 : this.x][z];
                visibleFaces[1] = neighbor == null || !neighbor.blocks[0][y][z].isSolid;
            } else {
                visibleFaces[1] = !blocks[x + 1][y][z].isSolid;
            }
            
            // Check negative Y
            visibleFaces[2] = y == 0 || !blocks[x][y - 1][z].isSolid;
            
            // Check positive Y
            visibleFaces[3] = y == CHUNK_HEIGHT - 1 || !blocks[x][y + 1][z].isSolid;
            
            // Check negative Z
            if (z == 0) {
                // Check neighboring chunk
                if (worldZ - 1 >= 0) {
                    Chunk neighbor = chunks[x][z > 0 ? this.z - 1 : this.z];
                    visibleFaces[4] = neighbor == null || !neighbor.blocks[x][y][CHUNK_SIZE - 1].isSolid;
                } else {
                    visibleFaces[4] = true;
                }
            } else {
                visibleFaces[4] = !blocks[x][y][z - 1].isSolid;
            }
            
            // Check positive Z
            if (z == CHUNK_SIZE - 1) {
                // Check neighboring chunk
                Chunk neighbor = chunks[x][z < CHUNK_SIZE - 1 ? this.z + 1 : this.z];
                visibleFaces[5] = neighbor == null || !neighbor.blocks[x][y][0].isSolid;
            } else {
                visibleFaces[5] = !blocks[x][y][z + 1].isSolid;
            }
            
            // Add vertices for visible faces
            for (int face = 0; face < 6; face++) {
                if (visibleFaces[face]) {
                    addFaceVertices(vertices, worldX, y, worldZ, face, type);
                }
            }
        }
        
        private void addFaceVertices(List<Float> vertices, float x, float y, float z, int face, BlockType type) {
            // Define cube vertices (centered at origin)
            float[][] cubeVertices = {
                // Front face
                {-0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, 0.5f}, {0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f},
                // Back face
                {0.5f, -0.5f, -0.5f}, {-0.5f, -0.5f, -0.5f}, {-0.5f, 0.5f, -0.5f}, {0.5f, 0.5f, -0.5f},
                // Right face
                {0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, -0.5f}, {0.5f, 0.5f, -0.5f}, {0.5f, 0.5f, 0.5f},
                // Left face
                {-0.5f, -0.5f, -0.5f}, {-0.5f, -0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, -0.5f},
                // Top face
                {-0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, -0.5f}, {-0.5f, 0.5f, -0.5f},
                // Bottom face
                {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, 0.5f}, {-0.5f, -0.5f, 0.5f}
            };
            
            // Define texture coordinates (each face uses the same coords)
            float[][] texCoords = {
                {0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}
            };
            
            // Define normals for each face
            float[][] normals = {
                {0.0f, 0.0f, 1.0f},  // Front
                {0.0f, 0.0f, -1.0f}, // Back
                {1.0f, 0.0f, 0.0f},  // Right
                {-1.0f, 0.0f, 0.0f}, // Left
                {0.0f, 1.0f, 0.0f},  // Top
                {0.0f, -1.0f, 0.0f}  // Bottom
            };
            
            // Get the vertices for this face
            int startIndex = face * 4;
            
            // Add two triangles for the face
            for (int i = 0; i < 6; i++) {
                int vertexIndex;
                if (i == 0 || i == 1 || i == 3) {
                    // First triangle: 0, 1, 2
                    vertexIndex = i == 3 ? 2 : i;
                } else {
                    // Second triangle: 0, 2, 3
                    vertexIndex = i == 4 ? 0 : (i == 5 ? 2 : 3);
                }
                
                // Position
                vertices.add(x + cubeVertices[startIndex + vertexIndex][0]);
                vertices.add(y + cubeVertices[startIndex + vertexIndex][1]);
                vertices.add(z + cubeVertices[startIndex + vertexIndex][2]);
                
                // Texture coordinates (based on block type)
                float texX = (type.id % 16) / 16.0f;
                float texY = (type.id / 16) / 16.0f;
                vertices.add(texX + texCoords[vertexIndex][0] / 16.0f);
                vertices.add(texY + texCoords[vertexIndex][1] / 16.0f);
                
                // Normal
                vertices.add(normals[face][0]);
                vertices.add(normals[face][1]);
                vertices.add(normals[face][2]);
            }
        }
        
        public void render() {
            if (vertexCount > 0) {
                glBindVertexArray(vao);
                glDrawArrays(GL_TRIANGLES, 0, vertexCount);
                glBindVertexArray(0);
            }
        }
        
        public void cleanup() {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
        }
    }
    
    // Simple 3D vector class
    class Vector3f {
        public float x, y, z;
        
        public Vector3f(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    // Simple 4x4 matrix class
    class Matrix4f {
        public float[] elements = new float[16];
        
        public Matrix4f() {
            identity();
        }
        
        public void identity() {
            for (int i = 0; i < 16; i++) {
                elements[i] = 0.0f;
            }
            elements[0] = 1.0f;
            elements[5] = 1.0f;
            elements[10] = 1.0f;
            elements[15] = 1.0f;
        }
        
        public void translate(float x, float y, float z) {
            elements[3] += x;
            elements[7] += y;
            elements[11] += z;
        }
        
        public void rotate(float angle, float x, float y, float z) {
            float c = (float) Math.cos(angle);
            float s = (float) Math.sin(angle);
            float ic = 1.0f - c;
            
            elements[0] = x * x * ic + c;
            elements[1] = x * y * ic - z * s;
            elements[2] = x * z * ic + y * s;
            
            elements[4] = y * x * ic + z * s;
            elements[5] = y * y * ic + c;
            elements[6] = y * z * ic - x * s;
            
            elements[8] = z * x * ic - y * s;
            elements[9] = z * y * ic + x * s;
            elements[10] = z * z * ic + c;
        }
    }
    
    // Simple Perlin/Simplex noise implementation
    class SimplexNoise {
        private static final int[] perm = new int[512];
        
        static {
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) p[i] = i;
            
            // Shuffle the array
            Random random = new Random();
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256);
                int tmp = p[i];
                p[i] = p[j];
                p[j] = tmp;
            }
            
            // Duplicate the permutation vector
            for (int i = 0; i < 512; i++) {
                perm[i] = p[i & 255];
            }
        }
        
        private int seed;
        
        public SimplexNoise(int seed) {
            this.seed = seed;
        }
        
        public double eval(double x, double y) {
            // Simple 2D noise implementation
            int X = (int) Math.floor(x) & 255;
            int Y = (int) Math.floor(y) & 255;
            
            x -= Math.floor(x);
            y -= Math.floor(y);
            
            double u = fade(x);
            double v = fade(y);
            
            int A = perm[X] + Y;
            int AA = perm[A & 255];
            int AB = perm[(A + 1) & 255];
            int B = perm[(X + 1) & 255] + Y;
            int BA = perm[B & 255];
            int BB = perm[(B + 1) & 255];
            
            return lerp(v, lerp(u, grad(perm[AA & 255], x, y),
                            grad(perm[BA & 255], x - 1, y)),
                    lerp(u, grad(perm[AB & 255], x, y - 1),
                            grad(perm[BB & 255], x - 1, y - 1)));
        }
        
        private double fade(double t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
        
        private double lerp(double t, double a, double b) {
            return a + t * (b - a);
        }
        
        private double grad(int hash, double x, double y) {
            int h = hash & 7;
            double u = h < 4 ? x : y;
            double v = h < 4 ? y : x;
            return ((h & 1) != 0 ? -u : u) + ((h & 2) != 0 ? -2.0 * v : 2.0 * v);
        }
    }
    
    // Main method
    public static void main(String[] args) {
        new MinecraftClone().run();
    }
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        
        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Minecraft Clone", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup input callbacks
        inputHandler = new InputHandler();
        glfwSetKeyCallback(window, inputHandler::keyCallback);
        glfwSetCursorPosCallback(window, inputHandler::cursorPosCallback);
        
        // Hide cursor and capture it
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Make the window visible
        glfwShowWindow(window);
        
        // Initialize OpenGL
        GL.createCapabilities();
        
        // Set the clear color
        glClearColor(0.6f, 0.8f, 1.0f, 1.0f);
        
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        
        // Enable face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Create shader program
        shaderProgram = createShaderProgram();
        
        // Load texture atlas
        textureAtlas = loadTexture("texture_atlas.png");
        
        // Initialize camera
        camera = new Camera(0, 70, 0);
        
        // Generate world
        generateWorld();
    }
    
    private void generateWorld() {
        int worldSize = RENDER_DISTANCE * 2 + 1;
        chunks = new Chunk[worldSize][worldSize];
        
        for (int x = -RENDER_DISTANCE; x <= RENDER_DISTANCE; x++) {
            for (int z = -RENDER_DISTANCE; z <= RENDER_DISTANCE; z++) {
                int arrayX = x + RENDER_DISTANCE;
                int arrayZ = z + RENDER_DISTANCE;
                chunks[arrayX][arrayZ] = new Chunk(x, z);
            }
        }
    }
    
    private int createShaderProgram() {
        // Vertex shader source
        String vertexShaderSource = "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "layout (location = 2) in vec3 aNormal;\n" +
            "out vec2 TexCoord;\n" +
            "out vec3 Normal;\n" +
            "out vec3 FragPos;\n" +
            "uniform mat4 model;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 projection;\n" +
            "void main() {\n" +
            "   gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
            "   TexCoord = aTexCoord;\n" +
            "   Normal = aNormal;\n" +
            "   FragPos = vec3(model * vec4(aPos, 1.0));\n" +
            "}\n";
        
        // Fragment shader source
        String fragmentShaderSource = "#version 330 core\n" +
            "out vec4 FragColor;\n" +
            "in vec2 TexCoord;\n" +
            "in vec3 Normal;\n" +
            "in vec3 FragPos;\n" +
            "uniform sampler2D textureAtlas;\n" +
            "uniform vec3 lightPos;\n" +
            "uniform vec3 viewPos;\n" +
            "void main() {\n" +
            "   // Ambient light\n" +
            "   float ambientStrength = 0.3;\n" +
            "   vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);\n" +
            "   \n" +
            "   // Diffuse light\n" +
            "   vec3 norm = normalize(Normal);\n" +
            "   vec3 lightDir = normalize(lightPos - FragPos);\n" +
            "   float diff = max(dot(norm, lightDir), 0.0);\n" +
            "   vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);\n" +
            "   \n" +
            "   // Specular light (simple)\n" +
            "   float specularStrength = 0.5;\n" +
            "   vec3 viewDir = normalize(viewPos - FragPos);\n" +
            "   vec3 reflectDir = reflect(-lightDir, norm);\n" +
            "   float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);\n" +
            "   vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);\n" +
            "   \n" +
            "   // Combine results\n" +
            "   vec3 result = (ambient + diffuse + specular) * texture(textureAtlas, TexCoord).rgb;\n" +
            "   FragColor = vec4(result, 1.0);\n" +
            "}\n";
        
        // Compile shaders
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkShaderCompileErrors(vertexShader, "VERTEX");
        
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkShaderCompileErrors(fragmentShader, "FRAGMENT");
        
        // Create shader program
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramLinkErrors(program);
        
        // Delete shaders
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        return program;
    }
    
    private void checkShaderCompileErrors(int shader, String type) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            String infoLog = glGetShaderInfoLog(shader);
            System.err.println("SHADER_COMPILATION_ERROR of type: " + type + "\n" + infoLog);
        }
    }
    
    private void checkProgramLinkErrors(int program) {
        int success = glGetProgrami(program, GL_LINK_STATUS);
        if (success == GL_FALSE) {
            String infoLog = glGetProgramInfoLog(program);
            System.err.println("PROGRAM_LINKING_ERROR\n" + infoLog);
        }
    }
    
    private int loadTexture(String path) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        
        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // Load image
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Load texture data
            ByteBuffer image = stbi_load(path, width, height, channels, 4);
            if (image != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
                stbi_image_free(image);
            } else {
                throw new RuntimeException("Failed to load texture: " + path);
            }
        }
        
        return texture;
    }
    
    private void loop() {
        double lastTime = glfwGetTime();
        
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;
            
            // Update game state
            update(deltaTime);
            
            // Render
            render();
            
            // Poll for window events
            glfwPollEvents();
        }
    }
    
    private void update(float deltaTime) {
        camera.update(deltaTime);
    }
    
    private void render() {
        // Clear the framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Use our shader program
        glUseProgram(shaderProgram);
        
        // Bind texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        glUniform1i(glGetUniformLocation(shaderProgram, "textureAtlas"), 0);
        
        // Set up projection matrix
        Matrix4f projection = new Matrix4f();
        // Create a perspective projection
        float aspect = (float) WIDTH / (float) HEIGHT;
        float fov = (float) Math.toRadians(70.0f);
        float near = 0.1f;
        float far = 1000.0f;
        
        float yScale = (float) (1.0f / Math.tan(fov / 2.0f));
        float xScale = yScale / aspect;
        float frustumLength = far - near;
        
        projection.elements[0] = xScale;
        projection.elements[5] = yScale;
        projection.elements[10] = -((far + near) / frustumLength);
        projection.elements[11] = -1.0f;
        projection.elements[14] = -((2.0f * near * far) / frustumLength);
        projection.elements[15] = 0.0f;
        
        // Set up view matrix (from camera)
        Matrix4f view = camera.getViewMatrix();
        
        // Set up model matrix (identity for world)
        Matrix4f model = new Matrix4f();
        
        // Pass matrices to shader
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, projection.elements);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"), false, view.elements);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "model"), false, model.elements);
        
        // Pass lighting parameters to shader
        glUniform3f(glGetUniformLocation(shaderProgram, "lightPos"), 100.0f, 100.0f, 100.0f);
        glUniform3f(glGetUniformLocation(shaderProgram, "viewPos"), camera.x, camera.y, camera.z);
        
        // Render all chunks
        for (int x = 0; x < chunks.length; x++) {
            for (int z = 0; z < chunks[x].length; z++) {
                if (chunks[x][z] != null) {
                    chunks[x][z].render();
                }
            }
        }
        
        // Swap the color buffers
        glfwSwapBuffers(window);
    }
    
    private void cleanup() {
        // Clean up chunks
        for (int x = 0; x < chunks.length; x++) {
            for (int z = 0; z < chunks[x].length; z++) {
                if (chunks[x][z] != null) {
                    chunks[x][z].cleanup();
                }
            }
        }
        
        // Clean up shaders and textures
        glDeleteProgram(shaderProgram);
        glDeleteTextures(textureAtlas);
        
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

}