# ANDROID COMPOSER

## App Overview
Android Composer is an innovative app that generates unique music pieces by concatenating multiple random short audio clips from the internet. Designed for professional composers seeking creative inspiration, the app is built using Kotlin and leverages **LiveData** and **ViewModel** to manage data across multiple activities and fragments. The app's backend is powered by **Firebase**'s services such as **Authentication**, **Firestore Database**, and **Storage**.

## Video Demo
[Android Composer](https://youtu.be/BDDbA1enK_c)

## App User Interface
<!-- |Composer|My Music|User Login Interface|
|----|----|----|
|![composer](Screenshot_composer.png)|![mymusic](Screenshot_mymusic.png)|![signin](Screenshot_signin.png)| -->
<table>
<th>
Composer
</th>
<th>
My Music
</th>
<th>
User Login Interface
</th>
<tr>
<td>
<img src="./Screenshot_composer.png" width=200px />
</td>
<td>
<img src="./Screenshot_mymusic.png" width=200px />
</td>
<td>
<img src="./Screenshot_signin.png" width=200px />
</td>
</tr>
</table>


## API
### Freesound
Freesound aims to create a huge collaborative database of audio snippets, samples, recordings, bleeps, ... released under Creative Commons licenses that allow their reuse. Freesound provides new and interesting ways of accessing these samples.   
Freesound also aims to create an open database of sounds that can also be used for scientific research. Many audio research institutions have trouble finding correctly licensed audio to test their algorithms. 

### Firebase 
- Authentication
- Firestore Database 
- Storage

## Android Features
### MediaPlayer
- MediaPlayer is used for users to play either audio snippets or the music pieces that are user-created 
- Audio snippets are played as local files downloaded from Firebase Storage
- Music pieces are played using the URL from Firebase Storage without the document actually being downloaded to the local device
### Bottom Navigation 
- The app has a bottom navigation (two destinations: “Composer” and “My Music”)
- User can toggle between the two destinations at any time

### Action Bar 
- Title: displays the title of the fragment (“Composer” or “My Music”)
- Sign out : users can sign out at any time. This is a menu item in the action bar.
