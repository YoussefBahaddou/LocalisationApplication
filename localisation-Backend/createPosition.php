<?php
if($_SERVER["REQUEST_METHOD"] == "POST"){
    include_once 'service/PositionService.php';
    create();
}

function create(){
    $latitude = $_POST['latitude'];
    $longitude = $_POST['longitude'];
    $date = $_POST['date'];
    $imei = $_POST['imei'];
    
    $ss = new PositionService();
    $ss->create(new Position(1, $latitude, $longitude, $date, $imei));
}
