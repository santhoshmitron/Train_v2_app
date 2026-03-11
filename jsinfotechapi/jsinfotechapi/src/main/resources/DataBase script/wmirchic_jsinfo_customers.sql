-- MySQL dump 10.13  Distrib 5.7.27, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: wmirchic_jsinfo
-- ------------------------------------------------------
-- Server version	5.7.27-0ubuntu0.16.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `mobile` varchar(100) NOT NULL,
  `phone` varchar(50) NOT NULL,
  `address` varchar(100) NOT NULL,
  `city` varchar(100) NOT NULL,
  `pincode` varchar(10) NOT NULL,
  `pic` varchar(500) NOT NULL DEFAULT 'profile.png',
  `type` varchar(25) NOT NULL DEFAULT 'user',
  `shop_name` varchar(100) NOT NULL,
  `added_on` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `target` int(11) NOT NULL DEFAULT '10000000',
  `status` varchar(25) NOT NULL DEFAULT 'Active',
  `logo` varchar(255) NOT NULL,
  `roles` varchar(10) NOT NULL DEFAULT 'GM',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=MyISAM AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (1,'sm1','123123','Mr Sundaralingam ','sm1@jsinfotech.com','+919945992045','08025441821','64, SJ School Road','Bangalore','560056','1564474863photojpen117300x450.jpg','user','JS InfoTech','2019-07-27 10:01:13',0,'Active','1564472222plasticidcard500x500.jpg','SM'),(2,'gm1','123123','Ramesh','rameshkumr@gmail.com','9945992045','08025441821','JP Nagar','Bangalore','560055','1564474808PassportSizePhoto.jpg','user','sm1','2019-07-30 13:45:30',0,'Active','1564474700SafetyPassportIDCardJohnDoeFront.png','GM'),(3,'gm2','123123','kumar','','','','','Bangalore','','profile.png','user','sm1','2019-08-05 13:17:56',10000000,'Active','','GM'),(4,'gm3','123123','Prashanth','','','','','Bangalore','','profile.png','user','sm1','2019-08-05 13:18:26',10000000,'Active','','GM'),(5,'Test','123123','query','test@jsnfotech.co','2555555555','155225522','hyd','hyd','523167','profile.png','user','jsipl','2019-09-07 15:48:42',0,'Active','','GM'),(6,'jsipl','123123','info','j@jsinfoech.co','777777777','45555','hyd','ban','523523','profile.png','user','ban','2019-09-07 16:09:57',10000000,'Active','','SM'),(7,'Test1','123123','query1','t1@gmail.com','22222','','88nbhj','hyd','22222','profile.png','user','jsipl','2019-09-18 14:38:02',0,'Active','','GM');
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-10-24 13:56:57
