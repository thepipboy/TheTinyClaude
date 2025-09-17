import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class RLWebCrawler extends JPanel {
    // RL Parameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_RATE = 0.3;
    private static final int STATE_VECTOR_SIZE = 10;
    private static final int ACTION_SPACE_SIZE = 5;
    
    // Q-Table for reinforcement learning
    private Map<String, double[]> qTable = new ConcurrentHashMap<>();
    private Random random = new Random();
    
    // Minecraft visualization
    private BufferedImage minecraftView;
    private int playerX = 0, playerY = 0;
    private final int BLOCK_SIZE = 32;
    private final int VIEW_SIZE = 15; // 15x15 grid
    
    // Web crawler components
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger pageCount = new AtomicInteger(0);
    private final ReentrantLock qTableLock = new ReentrantLock();
    
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    
    // Domain knowledge for RL
    private Set<String> valuableDomains = new HashSet<>(Arrays.asList(
        "wikipedia.org", "github.com", "stackoverflow.com", "arxiv.org"
    ));
    
    // Action definitions
    private static final int ACTION_CRAWL_DEEPER = 0;
    private static final int ACTION_BROADEN_SCOPE = 1;
    private static final int ACTION_FOCUS_DOMAIN = 2;
    private static final int ACTION_PRIORITIZE_NEW = 3;
    private static final int ACTION_BALANCED_APPROACH = 4;
    
    public RLWebCrawler() {
        setPreferredSize(new Dimension(VIEW_SIZE * BLOCK_SIZE, VIEW_SIZE * BLOCK_SIZE));
        minecraftView = new BufferedImage(VIEW_SIZE * BLOCK_SIZE, VIEW_SIZE * BLOCK_SIZE, 
                                         BufferedImage.TYPE_INT_RGB);
        initializeMinecraftWorld();
        
        // Start the crawler in a separate thread
        new Thread(this::startCrawling).start();
        
        // Start the visualization timer
        new Timer(100, e -> repaint()).start();
    }
    
    private void initializeMinecraftWorld() {
        Graphics2D g = minecraftView.createGraphics();
        
        // Draw basic terrain
        for (int x = 0; x < VIEW_SIZE; x++) {
            for (int y = 0; y < VIEW_SIZE; y++) {
                if (y > VIEW_SIZE / 2) {
                    // Stone layer
                    g.setColor(new Color(100, 100, 100));
                } else if (y == VIEW_SIZE / 2) {
                    // Grass layer
                    g.setColor(new Color(0, 150, 0));
                } else {
                    // Sky
                    g.setColor(new Color(135, 206, 235));
                }
                g.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                
                // Grid lines
                g.setColor(Color.BLACK);
                g.drawRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            }
        }
        
        // Add some blocks to represent web pages
        for (int i = 0; i < 10; i++) {
            int x = random.nextInt(VIEW_SIZE);
            int y = random.nextInt(VIEW_SIZE / 2 - 2);
            g.setColor(new Color(200, 100 + random.nextInt(100), 50));
            g.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        }
        
        g.dispose();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(minecraftView, 0, 0, this);
        
        // Draw player
        g.setColor(Color.RED);
        g.fillRect(playerX * BLOCK_SIZE, playerY * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        
        // Draw status information
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 40);
        g.setColor(Color.BLACK);
        g.drawString("Pages Crawled: " + pageCount.get(), 10, 15);
        g.drawString("URLs in Queue: " + urlQueue.size(), 10, 30);
    }
    
    private void startCrawling() {
        // Start with some seed URLs
        urlQueue.add("https://en.wikipedia.org/wiki/Main_Page");
        urlQueue.add("https://github.com");
        urlQueue.add("https://stackoverflow.com");
        
        while (pageCount.get() < 1000) { // Limit to 1000 pages for demo
            try {
                String url = urlQueue.poll(1, TimeUnit.SECONDS);
                if (url == null) continue;
                
                // Choose action based on RL policy
                int action = chooseAction(url);
                
                // Process the URL with the chosen action
                processUrl(url, action);
                
                // Update Minecraft visualization
                updateMinecraftView(url, action);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private int chooseAction(String url) {
        String state = extractState(url);
        double[] actionValues = qTable.getOrDefault(state, new double[ACTION_SPACE_SIZE]);
        
        // Exploration vs exploitation
        if (random.nextDouble() < EXPLORATION_RATE) {
            return random.nextInt(ACTION_SPACE_SIZE);
        } else {
            return argMax(actionValues);
        }
    }
    
    private String extractState(String url) {
        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            
            // Create a state representation based on URL characteristics
            StringBuilder state = new StringBuilder();
            
            // Domain quality (simplified)
            state.append(valuableDomains.contains(domain) ? "1" : "0");
            
            // URL depth (number of path segments)
            String path = parsedUrl.getPath();
            int depth = path.split("/").length - 1;
            state.append(Math.min(depth, 9)); // Cap at 9
            
            // URL length category
            int length = url.length();
            state.append(Math.min(length / 20, 9)); // Capped at 9
            
            // Contains keywords
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("wiki") || lowerUrl.contains("documentation")) state.append("1");
            else state.append("0");
            
            // File type
            if (url.endsWith(".pdf") || url.endsWith(".doc")) state.append("1");
            else state.append("0");
            
            // Pad with zeros if needed
            while (state.length() < STATE_VECTOR_SIZE) {
                state.append("0");
            }
            
            return state.toString();
        } catch (MalformedURLException e) {
            return "0000000000"; // Default state for malformed URLs
        }
    }
    
    private void processUrl(String url, int action) {
        if (visitedUrls.contains(url)) return;
        visitedUrls.add(url);
        
        String htmlContent = fetchPage(url);
        if (htmlContent == null) return;
        
        int reward = calculateReward(url, htmlContent, action);
        Set<String> discoveredUrls = extractLinks(htmlContent, url);
        
        // Apply action strategy to discovered URLs
        applyActionStrategy(discoveredUrls, action);
        
        // Update Q-table
        updateQTable(url, action, reward, discoveredUrls.size());
        
        pageCount.incrementAndGet();
        
        // Add discovered URLs to queue based on strategy
        for (String discoveredUrl : discoveredUrls) {
            if (!visitedUrls.contains(discoveredUrl)) {
                urlQueue.offer(discoveredUrl);
            }
        }
    }
    
    private int calculateReward(String url, String content, int action) {
        int reward = 0;
        
        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            
            // Reward for valuable domains
            if (valuableDomains.contains(domain)) reward += 10;
            
            // Reward for content quality (simplified)
            if (content.length() > 1000) reward += 5;
            if (content.contains("research") || content.contains("study")) reward += 3;
            
            // Reward for discovering many links
            Set<String> links = extractLinks(content, url);
            reward += Math.min(links.size() / 5, 5);
            
            // Penalty for low-quality content
            if (content.length() < 200) reward -= 2;
            
        } catch (MalformedURLException e) {
            reward -= 5;
        }
        
        return reward;
    }
    
    private void applyActionStrategy(Set<String> urls, int action) {
        List<String> urlList = new ArrayList<>(urls);
        
        switch (action) {
            case ACTION_CRAWL_DEEPER:
                // Prioritize URLs from the same domain
                urls.clear();
                for (String u : urlList) {
                    try {
                        if (new URL(u).getHost().equals(new URL(urlList.get(0)).getHost())) {
                            urls.add(u);
                        }
                    } catch (MalformedURLException e) {
                        // Skip malformed URLs
                    }
                }
                break;
                
            case ACTION_BROADEN_SCOPE:
                // Prioritize URLs from new domains
                Set<String> domains = new HashSet<>();
                urls.clear();
                for (String u : urlList) {
                    try {
                        String domain = new URL(u).getHost();
                        if (!domains.contains(domain)) {
                            urls.add(u);
                            domains.add(domain);
                        }
                    } catch (MalformedURLException e) {
                        // Skip malformed URLs
                    }
                }
                break;
                
            case ACTION_FOCUS_DOMAIN:
                // Prioritize URLs from valuable domains
                urls.clear();
                for (String u : urlList) {
                    try {
                        String domain = new URL(u).getHost();
                        if (valuableDomains.contains(domain)) {
                            urls.add(u);
                        }
                    } catch (MalformedURLException e) {
                        // Skip malformed URLs
                    }
                }
                break;
                
            case ACTION_PRIORITIZE_NEW:
                // No filtering - keep all URLs
                break;
                
            case ACTION_BALANCED_APPROACH:
                // Mix of strategies - keep a balanced set
                if (urlList.size() > 20) {
                    // If too many URLs, sample a diverse set
                    Collections.shuffle(urlList);
                    urls.clear();
                    for (int i = 0; i < Math.min(20, urlList.size()); i++) {
                        urls.add(urlList.get(i));
                    }
                }
                break;
        }
    }
    
    private void updateQTable(String url, int action, int reward, int discoveredUrls) {
        String state = extractState(url);
        String nextState = state; // Simplified - in reality would be based on next URL
        
        qTableLock.lock();
        try {
            double[] actionValues = qTable.getOrDefault(state, new double[ACTION_SPACE_SIZE]);
            double[] nextActionValues = qTable.getOrDefault(nextState, new double[ACTION_SPACE_SIZE]);
            
            // Q-learning update
            double oldValue = actionValues[action];
            double nextMax = Arrays.stream(nextActionValues).max().orElse(0);
            
            double newValue = oldValue + LEARNING_RATE * 
                (reward + DISCOUNT_FACTOR * nextMax - oldValue);
            
            actionValues[action] = newValue;
            qTable.put(state, actionValues);
            
        } finally {
            qTableLock.unlock();
        }
    }
    
    private void updateMinecraftView(String url, int action) {
        // Update player position based on crawling activity
        playerX = (playerX + 1) % VIEW_SIZE;
        playerY = (playerY + (action % 2 == 0 ? 1 : -1)) % VIEW_SIZE;
        if (playerY < 0) playerY = VIEW_SIZE - 1;
        
        // Add a new block to represent the crawled page
        Graphics2D g = minecraftView.createGraphics();
        
        // Determine block color based on action and URL quality
        Color blockColor;
        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            
            if (valuableDomains.contains(domain)) {
                blockColor = new Color(0, 100 + action * 30, 200); // Blue-green shades
            } else {
                blockColor = new Color(150 + action * 20, 100, 50); // Red-orange shades
            }
        } catch (MalformedURLException e) {
            blockColor = Color.GRAY;
        }
        
        // Place the block at a random position in the upper part of the world
        int blockX = random.nextInt(VIEW_SIZE);
        int blockY = random.nextInt(VIEW_SIZE / 2 - 2);
        
        g.setColor(blockColor);
        g.fillRect(blockX * BLOCK_SIZE, blockY * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        g.setColor(Color.BLACK);
        g.drawRect(blockX * BLOCK_SIZE, blockY * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        
        g.dispose();
        
        repaint();
    }
    
    private String fetchPage(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "RLWebCrawler/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            return content.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private Set<String> extractLinks(String htmlContent, String baseUrl) {
        Set<String> links = new HashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(htmlContent);
        
        try {
            URL base = new URL(baseUrl);
            
            while (matcher.find()) {
                String link = matcher.group(2);
                
                if (link.isEmpty() || link.startsWith("javascript:")) {
                    continue;
                }
                
                URL absoluteUrl = new URL(base, link);
                String normalizedUrl = normalizeUrl(absoluteUrl.toString());
                
                if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                    links.add(normalizedUrl);
                }
            }
        } catch (MalformedURLException e) {
            // Skip malformed URLs
        }
        
        return links;
    }
    
    private String normalizeUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String normalized = new URL(
                url.getProtocol(), 
                url.getHost(), 
                url.getPort(), 
                url.getFile()
            ).toString();
            
            normalized = normalized.replaceAll("(?i)[?&](utm_[^&]+|fbclid|gclid)=[^&]*", "");
            normalized = normalized.replaceAll("([?&])&+", "$1");
            normalized = normalized.replaceAll("\\?$", "");
            
            return normalized;
        } catch (MalformedURLException e) {
            return urlString;
        }
    }
    
    private int argMax(double[] values) {
        int maxIndex = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("RL Web Crawler with Minecraft Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new RLWebCrawler());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}