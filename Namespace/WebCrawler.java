import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler {
    // Configuration parameters
    private static final int MAX_PAGES = 100;
    private static final int NUM_THREADS = 5;
    private static final int CRAWL_DELAY = 1000; // milliseconds between requests to same domain
    private static final String USER_AGENT = "SimpleWebCrawler/1.0";
    
    // Data structures for tracking visited URLs and domains
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger pageCount = new AtomicInteger(0);
    
    // Pattern for matching URLs in HTML
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for matching robots.txt rules
    private static final Pattern ROBOTS_RULE_PATTERN = Pattern.compile(
        "(?i)(Allow|Disallow):\\s*(.*)"
    );
    
    // Main method to start the crawler
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java WebCrawler <starting_url> [max_pages]");
            System.exit(1);
        }
        
        String startUrl = args[0];
        int maxPages = MAX_PAGES;
        
        if (args.length > 1) {
            try {
                maxPages = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid max_pages parameter, using default: " + MAX_PAGES);
            }
        }
        
        WebCrawler crawler = new WebCrawler();
        crawler.crawl(startUrl, maxPages);
    }
    
    // Main crawl method
    public void crawl(String startUrl, int maxPages) {
        System.out.println("Starting crawler from: " + startUrl);
        System.out.println("Maximum pages to crawl: " + maxPages);
        
        urlQueue.add(startUrl);
        
        // Create thread pool for concurrent crawling
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        
        // Start worker threads
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new CrawlerWorker(maxPages));
        }
        
        executor.shutdown();
        
        try {
            // Wait for all threads to finish or timeout after 1 hour
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.out.println("Crawling timed out after 1 hour");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nCrawling completed!");
        System.out.println("Total pages crawled: " + pageCount.get());
        System.out.println("Total URLs discovered: " + visitedUrls.size());
    }
    
    // Worker class for concurrent crawling
    private class CrawlerWorker implements Runnable {
        private final int maxPages;
        
        public CrawlerWorker(int maxPages) {
            this.maxPages = maxPages;
        }
        
        @Override
        public void run() {
            while (pageCount.get() < maxPages) {
                try {
                    String url = urlQueue.poll(1, TimeUnit.SECONDS);
                    if (url == null) {
                        // No URLs in queue, check if we're done
                        if (pageCount.get() >= maxPages) break;
                        continue;
                    }
                    
                    // Process the URL
                    processUrl(url);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Log errors but continue with other URLs
                    System.err.println("Error in worker thread: " + e.getMessage());
                }
            }
        }
        
        private void processUrl(String urlString) {
            // Check if we've reached the page limit
            if (pageCount.get() >= maxPages) return;
            
            // Check if we've already visited this URL
            if (visitedUrls.contains(urlString)) return;
            
            // Respect robots.txt
            if (!isAllowedByRobots(urlString)) {
                System.out.println("Blocked by robots.txt: " + urlString);
                visitedUrls.add(urlString);
                return;
            }
            
            // Respect crawl delay for the domain
            String domain = getDomain(urlString);
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastRequestTime.get(domain);
            
            if (lastTime != null) {
                long timeSinceLastRequest = currentTime - lastTime;
                if (timeSinceLastRequest < CRAWL_DELAY) {
                    try {
                        Thread.sleep(CRAWL_DELAY - timeSinceLastRequest);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            
            lastRequestTime.put(domain, System.currentTimeMillis());
            
            // Fetch the page content
            String htmlContent = fetchPage(urlString);
            if (htmlContent == null) {
                visitedUrls.add(urlString);
                return;
            }
            
            // Mark URL as visited
            visitedUrls.add(urlString);
            int count = pageCount.incrementAndGet();
            
            // Extract and print page title
            String title = extractTitle(htmlContent);
            System.out.println(count + ". [" + domain + "] " + 
                              (title.isEmpty() ? urlString : title));
            
            // Extract and queue new URLs
            Set<String> discoveredUrls = extractLinks(htmlContent, urlString);
            for (String discoveredUrl : discoveredUrls) {
                if (!visitedUrls.contains(discoveredUrl) && pageCount.get() < maxPages) {
                    urlQueue.offer(discoveredUrl);
                }
            }
        }
    }
    
    // Fetch the content of a web page
    private String fetchPage(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("HTTP " + responseCode + " for URL: " + urlString);
                return null;
            }
            
            // Read the response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            return content.toString();
            
        } catch (Exception e) {
            System.err.println("Error fetching URL " + urlString + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // Extract links from HTML content
    private Set<String> extractLinks(String htmlContent, String baseUrl) {
        Set<String> links = new HashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(htmlContent);
        
        try {
            URL base = new URL(baseUrl);
            
            while (matcher.find()) {
                String link = matcher.group(2);
                
                // Skip empty links and JavaScript links
                if (link.isEmpty() || link.startsWith("javascript:")) {
                    continue;
                }
                
                // Convert relative URLs to absolute URLs
                URL absoluteUrl = new URL(base, link);
                String normalizedUrl = normalizeUrl(absoluteUrl.toString());
                
                // Only follow HTTP/HT URLs
                if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                    links.add(normalizedUrl);
                }
            }
        } catch (MalformedURLException e) {
            System.err.println("Error processing links from: " + baseUrl);
        }
        
        return links;
    }
    
    // Normalize URL by removing fragments and some query parameters
    private String normalizeUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            
            // Remove fragment
            String normalized = new URL(
                url.getProtocol(), 
                url.getHost(), 
                url.getPort(), 
                url.getFile()
            ).toString();
            
            // Remove common tracking parameters
            normalized = normalized.replaceAll("(?i)[?&](utm_[^&]+|fbclid|gclid)=[^&]*", "");
            normalized = normalized.replaceAll("([?&])&+", "$1"); // Remove duplicate &
            normalized = normalized.replaceAll("\\?$", ""); // Remove trailing ?
            
            return normalized;
        } catch (MalformedURLException e) {
            return urlString;
        }
    }
    
    // Extract page title from HTML
    private String extractTitle(String htmlContent) {
        Pattern titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = titlePattern.matcher(htmlContent);
        
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("\\s+", " ");
        }
        
        return "";
    }
    
    // Get domain from URL
    private String getDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }
    
    // Check if URL is allowed by robots.txt
    private boolean isAllowedByRobots(String urlString) {
        try {
            URL url = new URL(urlString);
            String robotsUrl = url.getProtocol() + "://" + url.getHost() + "/robots.txt";
            
            // Check if we've already processed robots.txt for this domain
            String domain = url.getHost();
            if (visitedRobotsDomains.contains(domain)) {
                return robotsRules.getOrDefault(domain, Collections.emptyList())
                    .stream().allMatch(rule -> isUrlAllowedByRule(urlString, rule));
            }
            
            // Fetch robots.txt
            String robotsContent = fetchPage(robotsUrl);
            if (robotsContent == null) {
                // If we can't fetch robots.txt, assume everything is allowed
                visitedRobotsDomains.add(domain);
                return true;
            }
            
            // Parse robots.txt rules
            List<RobotsRule> rules = parseRobotsTxt(robotsContent);
            robotsRules.put(domain, rules);
            visitedRobotsDomains.add(domain);
            
            // Check if URL is allowed by all rules
            return rules.stream().allMatch(rule -> isUrlAllowedByRule(urlString, rule));
            
        } catch (MalformedURLException e) {
            return true; // If URL is malformed, allow it (it will fail later anyway)
        }
    }
    
    // Parse robots.txt content
    private List<RobotsRule> parseRobotsTxt(String robotsContent) {
        List<RobotsRule> rules = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(robotsContent));
        String line;
        
        try {
            boolean inUserAgentSection = false;
            boolean ourUserAgent = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Check for User-agent directive
                if (line.toLowerCase().startsWith("user-agent:")) {
                    inUserAgentSection = true;
                    String userAgent = line.substring(11).trim();
                    ourUserAgent = userAgent.equals("*") || userAgent.contains(USER_AGENT);
                } 
                // Check for rules
                else if (inUserAgentSection && ourUserAgent) {
                    Matcher matcher = ROBOTS_RULE_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String type = matcher.group(1);
                        String path = matcher.group(2).trim();
                        rules.add(new RobotsRule(type.equalsIgnoreCase("Allow"), path));
                    }
                }
                // Other directives reset the section
                else if (line.toLowerCase().startsWith("sitemap:") || 
                         line.toLowerCase().startsWith("crawl-delay:")) {
                    inUserAgentSection = false;
                    ourUserAgent = false;
                }
            }
        } catch (IOException e) {
            // Shouldn't happen with StringReader
        }
        
        return rules;
    }
    
    // Check if a URL is allowed by a robots.txt rule
    private boolean isUrlAllowedByRule(String urlString, RobotsRule rule) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            
            if (path.startsWith(rule.path)) {
                return rule.allow;
            }
        } catch (MalformedURLException e) {
            // If URL is malformed, assume it's not allowed
            return false;
        }
        
        // If path doesn't match this rule, it doesn't affect the URL
        return true;
    }
    // Data structures for robots.txt processing
    private final Set<String> visitedRobotsDomains = ConcurrentHashMap.newKeySet();
    private final Map<String, List<RobotsRule>> robotsRules = new ConcurrentHashMap<>();
    // Helper class for robots.txt rules
    private static class RobotsRule {
        final boolean allow;
        final String path;
        
        RobotsRule(boolean allow, String path) {
            this.allow = allow;
            this.path = path;
        }
    }
}