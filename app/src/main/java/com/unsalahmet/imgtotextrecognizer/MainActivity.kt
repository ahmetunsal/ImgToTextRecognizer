package com.unsalahmet.imgtotextrecognizer

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.unsalahmet.imgtotextrecognizer.databinding.ActivityMainBinding
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db : FirebaseFirestore
    private lateinit var textList : MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.ocrButton.setOnClickListener {

            // request the access to the read the image from the gallery
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        textList = mutableListOf()

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val clipData = data?.clipData
            val imageUris = ArrayList<Uri>()

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    imageUris.add(clipData.getItemAt(i).uri)
                }
            } else {
                data?.data?.let { imageUris.add(it) }
            }

            for (uri in imageUris) {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val image = InputImage.fromBitmap(bitmap, 0)
                val builder = TextRecognizerOptions.DEFAULT_OPTIONS

                val recognizer = TextRecognition.getClient(builder)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Process the recognized text
                        println(visionText.text)
                        textList.add(visionText.text)
                        binding.textView.text = visionText.text
                        binding.copyButton.visibility = android.view.View.VISIBLE
                        binding.saveButton.visibility = android.view.View.VISIBLE
                    }
                    .addOnFailureListener { e ->
                        // Handle the error
                        e.printStackTrace()
                    }
            }

            binding.copyButton.setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OCR Text", textList.joinToString("\n"))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            }

            binding.saveButton.setOnClickListener {
                createPdfWithText(textList, "/storage/emulated/0/Download/ocr_text.pdf")
                Toast.makeText(this,
                    getString(R.string.text_saved_as_pdf_in_your_downloads_folder), Toast.LENGTH_SHORT).show()

            }
        }
    }

    fun createPdfWithText(textList: List<String>, filePath: String) {
        // make UTF-8 encoding
        val pdfWriter = PdfWriter(filePath)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        for (text in textList) {
            val paragraph = Paragraph(text)
            paragraph.setTextAlignment(TextAlignment.JUSTIFIED)
            document.add(paragraph)
            document.add(Paragraph("\u000c")) // Add a new page for the next image text
        }

        document.close()
    }

    companion object {
        const val REQUEST_CODE = 0
    }

}