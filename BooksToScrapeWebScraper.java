package webscrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class BooksToScrapeWebScraper extends JFrame {
    private JTextArea textArea;
    private JProgressBar progressBar;
    private JButton scrapeButton;
    private JButton exitButton;
    private JTextField searchField;

    public BooksToScrapeWebScraper() {
        setTitle("Books to Scrape Web Scraper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create UI components
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Scraped Data"));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        scrapeButton = new JButton("Scrape Website");
        scrapeButton.setBackground(Color.GREEN);
        scrapeButton.setForeground(Color.BLACK);
        scrapeButton.addActionListener(e -> scrapeWebsite());

        exitButton = new JButton("Exit");
        exitButton.setBackground(Color.RED);
        exitButton.setForeground(Color.BLACK);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        searchField = new JTextField(20);
        JLabel searchLabel = new JLabel("Enter Book Title:");

        // Create layout and add components
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(searchLabel);
        inputPanel.add(searchField);
        inputPanel.add(scrapeButton);
        inputPanel.add(exitButton);

        add(scrollPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.NORTH);
    }

    private void scrapeWebsite() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a book title.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        textArea.setText("");
        progressBar.setValue(0);
        scrapeButton.setEnabled(false);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter("books.csv"));
                    writer.write("Title,Price,Rating,Product Information\n");

                    // Scrape the first few pages of the catalog
                    for (int currentPage = 1; currentPage <= 5; currentPage++) { // Limit to 5 pages for demonstration
                        String url = "http://books.toscrape.com/catalogue/page-" + currentPage + ".html";
                        Document doc = Jsoup.connect(url).timeout(10000).get(); // Set a timeout of 10 seconds
                        publish("Accessing URL: " + url);

                        Elements products = doc.select("article.product_pod");
                        boolean foundBook = false;

                        for (Element product : products) {
                            String title = product.select("h3 a").attr("title");

                            // Check if the title contains the search term (case insensitive)
                            if (title.toLowerCase().contains(searchTerm.toLowerCase())) {
                                foundBook = true; // Mark that we found at least one book
                                String price = product.select("p.price_color").text();
                                String rating = product.select("p.star-rating").attr("class").replace("star-rating ", "");
                                String productInfoUrl = product.select("h3 a").attr("href");

                                // Fetch product information
                                String fullProductInfoUrl = "http://books.toscrape.com/catalogue/" + productInfoUrl;
                                Document productInfoDoc = Jsoup.connect(fullProductInfoUrl).timeout(10000).get();
                                String productInfo = productInfoDoc.select("meta[name=description]").attr("content").trim();

                                // Log output
                                String output = String.format("Title: %s\nPrice: %s\nRating: %s\nProduct Info: %s\n\n", title, price, rating, productInfo);
                                publish(output); // Send data to process method

                                // Write to CSV
                                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n", title, price, rating, productInfo));
                            }
                        }

                        // Update progress bar
                        setProgress((currentPage * 100) / 5); // Assuming 5 pages

                        // If no book found in this page, notify the user
                        if (!foundBook) {
                            publish("No books found on page " + currentPage + " matching the title: " + searchTerm);
                        }
                    }

                    writer.close();
                    publish("Data successfully written to books.csv");

                } catch (IOException e) {
                    publish("Error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String text : chunks) {
                    textArea.append(text + "\n");
                }
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                scrapeButton.setEnabled(true);
                JOptionPane.showMessageDialog(BooksToScrapeWebScraper.this, "Scraping complete!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BooksToScrapeWebScraper scraper = new BooksToScrapeWebScraper();
            scraper.setVisible(true);
        });
    }
}