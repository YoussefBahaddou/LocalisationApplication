<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

try {
    $host = 'localhost';
    $port = 3307;  // Specify the port
    $dbname = 'localisation';
    $login = 'root';
    $password = ''; // Try with empty password first
    
    // Include port in the DSN string
    $conn = new PDO("mysql:host=$host;port=$port;dbname=$dbname", $login, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Connected successfully";
} catch(PDOException $e) {
    echo "Connection failed: " . $e->getMessage();
}
?>
