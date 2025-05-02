<?php

class Connexion {
    private $connextion;
    
    public function __construct() {
        $host = 'localhost';
        $port = 3307;
        $dbname = 'localisation';
        $login = 'root';
        $password = '';
        
        try {
            $this->connextion = new PDO("mysql:host=$host;port=$port;dbname=$dbname", $login, $password);
            $this->connextion->query("SET NAMES UTF8");
        } catch (Exception $e) {
            die('Erreur : ' . $e->getMessage());
        }
    }
    
    function getConnextion() {
        return $this->connextion;
    }
}
?>
