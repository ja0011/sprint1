const editBtn = document.getElementById('editBtn');
    const saveBtn = document.getElementById('saveBtn');
    const nameInput = document.getElementById('nameInput');
    const bioInput = document.getElementById('bioInput');
    const displayName = document.getElementById('displayName');
    const displayBio = document.getElementById('displayBio');
    const profilePicInput = document.getElementById('profilePicInput');
    const profilePreview = document.getElementById('profilePreview');

    // Edit Profile
    editBtn.addEventListener('click', () => {
      displayName.style.display = 'none';
      displayBio.style.display = 'none';
      profilePreview.style.opacity = 0.5; // dim old picture
      nameInput.value = displayName.textContent;
      bioInput.value = displayBio.textContent;
      nameInput.style.display = 'block';
      bioInput.style.display = 'block';
      profilePicInput.style.display = 'block';
      editBtn.style.display = 'none';
      saveBtn.style.display = 'block';
    });

    // Save changes
    saveBtn.addEventListener('click', () => {
      if(nameInput.value.trim() !== '') displayName.textContent = nameInput.value;
      if(bioInput.value.trim() !== '') displayBio.textContent = bioInput.value;
      displayName.style.display = 'block';
      displayBio.style.display = 'block';
      profilePreview.style.opacity = 1;
      nameInput.style.display = 'none';
      bioInput.style.display = 'none';
      profilePicInput.style.display = 'none';
      saveBtn.style.display = 'none';
      editBtn.style.display = 'block';
    });

    // Preview profile picture
    function previewImage(event) {
      const reader = new FileReader();
      reader.onload = function(){
        profilePreview.src = reader.result;
      }
      reader.readAsDataURL(event.target.files[0]);
    }